package kgu.developers.admin.submission.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import kgu.developers.admin.submission.presentation.response.SubmissionAdminListResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionArtifactAdminResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminDetailResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminListResponse;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.fileobject.exception.FileObjectNotFoundException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.submission.exception.SubmissionVersionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubmissionAdminFacade {

    private final MilestoneRepository milestoneRepository;
    private final SectionQueryService sectionQueryService;
    private final TeamRepository teamRepository;
    private final SubmissionQueryService submissionQueryService;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final SubmissionArtifactRepository submissionArtifactRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;

    // 팀은 그 마일스톤을 아직 한 번도 조회 안 했으면 Submission 행 자체가 없다(lazy get-or-create).
    // 그대로 findAllByMilestoneId만 쓰면 그런 팀이 목록에서 통째로 빠지므로, 분반의 팀 전체를
    // 기준으로 각자 get-or-create해서 빠짐없이 보여준다.
    public SubmissionAdminListResponse getSubmissionsByMilestone(Long milestoneId, String professorId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(milestone.getSectionId(), professorId)) {
            throw new AccessDeniedException("담당 분반의 제출만 조회할 수 있습니다.");
        }

        List<Team> teams = teamRepository.findAllBySectionId(milestone.getSectionId());
        List<SubmissionAdminResponse> contents = teams.stream()
                .map(team -> {
                    Submission submission = submissionQueryService.getOrCreateSubmission(team.getId(), milestoneId);
                    return SubmissionAdminResponse.of(
                            submission, team,
                            submissionQueryService.canSubmitNow(submission),
                            submissionQueryService.hasPendingReview(submission));
                })
                .toList();
        return SubmissionAdminListResponse.from(contents);
    }

    public SubmissionAdminResponse getSubmission(Long submissionId, String professorId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Team team = validateProfessorOwnsSubmission(submission, professorId);
        return SubmissionAdminResponse.of(
                submission, team,
                submissionQueryService.canSubmitNow(submission),
                submissionQueryService.hasPendingReview(submission));
    }

    public SubmissionVersionAdminListResponse getVersions(Long submissionId, String professorId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateProfessorOwnsSubmission(submission, professorId);
        return SubmissionVersionAdminListResponse.from(submissionVersionRepository.findAllBySubmissionId(submissionId));
    }

    public SubmissionVersionAdminDetailResponse getVersion(Long submissionId, int version, String professorId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateProfessorOwnsSubmission(submission, professorId);

        SubmissionVersion submissionVersion = submissionVersionRepository
                .findBySubmissionIdAndVersion(submissionId, version)
                .orElseThrow(SubmissionVersionNotFoundException::new);

        List<SubmissionArtifactAdminResponse> artifacts = submissionArtifactRepository
                .findAllByVersionId(submissionVersion.getId()).stream()
                .map(this::toArtifactResponse)
                .toList();

        return SubmissionVersionAdminDetailResponse.of(submissionVersion, artifacts);
    }

    // 화면의 "일괄 다운로드"는 팀별 최신 제출 기준이라 버전을 따로 안 받고 currentVersion을 쓴다.
    // FileObject 조회까지 트랜잭션 안에서 전부 끝내 확정 목록으로 만든 뒤, 실제 S3 다운로드는
    // 트랜잭션 밖(응답 스트리밍 시점)에서 fileStorage만 호출하도록 분리했다.
    public SubmissionArtifactsZipDownload downloadArtifactsZip(Long submissionId, String professorId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Team team = validateProfessorOwnsSubmission(submission, professorId);

        SubmissionVersion submissionVersion = submissionVersionRepository
                .findBySubmissionIdAndVersion(submissionId, submission.getCurrentVersion())
                .orElseThrow(SubmissionVersionNotFoundException::new);

        List<FileObject> fileObjects = submissionArtifactRepository
                .findAllByVersionId(submissionVersion.getId()).stream()
                .filter(artifact -> artifact.getType() == ArtifactType.FILE)
                .map(artifact -> fileObjectRepository.findById(artifact.getFileId())
                        .orElseThrow(FileObjectNotFoundException::new))
                .toList();

        String zipFileName = team.getName() + "-submission.zip";
        return new SubmissionArtifactsZipDownload(zipFileName, outputStream -> writeZip(fileObjects, outputStream));
    }

    private void writeZip(List<FileObject> fileObjects, OutputStream outputStream) {
        Set<String> usedNames = new HashSet<>();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (FileObject fileObject : fileObjects) {
                zipOutputStream.putNextEntry(new ZipEntry(uniqueEntryName(usedNames, fileObject.getFileName())));
                try (InputStream fileInputStream = fileStorage.download(fileObject.getStorageKey())) {
                    fileInputStream.transferTo(zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String uniqueEntryName(Set<String> usedNames, String fileName) {
        String candidate = (fileName == null || fileName.isBlank()) ? "file" : fileName;
        int suffix = 2;
        while (!usedNames.add(candidate)) {
            candidate = fileName + "-" + suffix++;
        }
        return candidate;
    }

    private SubmissionArtifactAdminResponse toArtifactResponse(SubmissionArtifact artifact) {
        if (artifact.getType() != ArtifactType.FILE) {
            return SubmissionArtifactAdminResponse.of(artifact);
        }
        FileObject fileObject = fileObjectRepository.findById(artifact.getFileId())
                .orElseThrow(FileObjectNotFoundException::new);
        String downloadUrl = fileStorage.presignedUrl(fileObject.getStorageKey());
        return SubmissionArtifactAdminResponse.ofFile(artifact, fileObject, downloadUrl);
    }

    private Team validateProfessorOwnsSubmission(Submission submission, String professorId) {
        Team team = teamRepository.findById(submission.getTeamId())
                .orElseThrow(TeamNotFoundException::new);
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(team.getSectionId(), professorId)) {
            throw new AccessDeniedException("담당 분반의 제출만 조회할 수 있습니다.");
        }
        return team;
    }
}
