package kgu.developers.admin.submission.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
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
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubmissionAdminFacade {

    private final MilestoneRepository milestoneRepository;
    private final SectionQueryService sectionQueryService;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final SubmissionQueryService submissionQueryService;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final SubmissionArtifactRepository submissionArtifactRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final UserQueryService userQueryService;
    private final MeetingRecordQueryService meetingRecordQueryService;

    // 팀은 그 마일스톤을 아직 한 번도 조회 안 했으면 Submission 행 자체가 없다(lazy get-or-create).
    // 그대로 findAllByMilestoneId만 쓰면 그런 팀이 목록에서 통째로 빠지므로, 분반의 팀 전체를
    // 기준으로 각자 get-or-create해서 빠짐없이 보여준다.
    public SubmissionAdminListResponse getSubmissionsByMilestone(
            Long milestoneId,
            Long teamId,
            String professorId
    ) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(milestone.getSectionId(), professorId)) {
            throw new AccessDeniedException("담당 분반의 제출만 조회할 수 있습니다.");
        }

        List<Team> teams = filterTeam(
                teamRepository.findAllBySectionId(milestone.getSectionId()), teamId);
        Map<Long, String> projectTitles = milestone.getType() == MilestoneType.PROPOSAL
                ? projectRepository.findAllByTeamIdIn(teams.stream().map(Team::getId).toList()).stream()
                        .collect(Collectors.toMap(Project::getTeamId, Project::getTitle, (first, ignored) -> first))
                : Map.of();
        Map<Long, Long> meetingRecordCounts = meetingRecordQueryService.countMeetingRecords(
                teams.stream().map(Team::getId).toList(), milestoneId);
        List<SubmissionAdminResponse> contents = teams.stream()
                .map(team -> {
                    Submission submission = submissionQueryService.getOrCreateSubmission(team.getId(), milestoneId);
                    return SubmissionAdminResponse.of(
                            submission, team,
                            submissionQueryService.canSubmitNow(submission),
                            submissionQueryService.hasPendingReview(submission),
                            projectTitles.get(team.getId()),
                            meetingRecordCounts.getOrDefault(team.getId(), 0L));
                })
                .toList();
        return SubmissionAdminListResponse.from(contents);
    }

    private List<Team> filterTeam(List<Team> teams, Long teamId) {
        if (teamId == null) {
            return teams;
        }
        return teams.stream()
                .filter(team -> team.getId().equals(teamId))
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new AccessDeniedException("담당 분반의 제출만 조회할 수 있습니다."));
    }

    public SubmissionAdminResponse getSubmission(Long submissionId, String professorId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Team team = validateProfessorOwnsSubmission(submission, professorId);
        String projectTitle = resolveProjectTitle(submission, team);
        return SubmissionAdminResponse.of(
                submission, team,
                submissionQueryService.canSubmitNow(submission),
                submissionQueryService.hasPendingReview(submission),
                projectTitle,
                meetingRecordQueryService.countMeetingRecords(team.getId(), submission.getMilestoneId()));
    }

    private String resolveProjectTitle(Submission submission, Team team) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (milestone.getType() != MilestoneType.PROPOSAL) {
            return null;
        }
        return projectRepository.findAllByTeamIdIn(List.of(team.getId())).stream()
                .map(Project::getTitle)
                .findFirst()
                .orElse(null);
    }

    public SubmissionVersionAdminListResponse getVersions(Long submissionId, String professorId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateProfessorOwnsSubmission(submission, professorId);

        List<SubmissionVersion> versions = submissionVersionRepository.findAllBySubmissionId(submissionId);
        return SubmissionVersionAdminListResponse.from(versions, resolveSubmitters(versions));
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

        User submitter = resolveSubmitters(List.of(submissionVersion)).get(submissionVersion.getSubmittedBy());
        return SubmissionVersionAdminDetailResponse.of(submissionVersion, submitter, artifacts);
    }

    // 제출 이력은 그 시점의 기록이라, 제출자가 그 뒤 탈퇴(소프트 삭제)했더라도 이름이 계속
    // 보여야 한다 — 활성 사용자만 찾는 조회를 쓰면 탈퇴한 제출자의 이름이 조용히 null이 된다
    // (KD3-164, aics-api SubmissionFacade.resolveSubmitters와 같은 이유·같은 패턴).
    private Map<String, User> resolveSubmitters(List<SubmissionVersion> versions) {
        List<String> submitterIds = versions.stream()
                .map(SubmissionVersion::getSubmittedBy)
                .distinct()
                .toList();
        if (submitterIds.isEmpty()) {
            return Map.of();
        }
        return userQueryService.getUsersByStudentNumbersIncludingDeleted(submitterIds).stream()
                .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
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
                String entryName = uniqueEntryName(usedNames, sanitizeEntryName(fileObject.getFileName()));
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                try (InputStream fileInputStream = fileStorage.download(fileObject.getStorageKey())) {
                    fileInputStream.transferTo(zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // DB에 저장된 원본 파일명은 업로드 시점에 클라이언트가 임의로 지정한 값이라 신뢰할 수 없다.
    // "../../etc/passwd"나 "..\\foo" 같은 값을 그대로 ZipEntry 이름에 쓰면, 압축 해제 환경에
    // 따라 지정한 폴더 밖에 파일을 쓰는 zip slip(경로 이탈) 공격이 될 수 있다(sunzx0428 PR #138
    // 리뷰 09-06). 경로 구분자를 전부 슬래시로 통일한 뒤 마지막 구성요소만 남기면 "../"류
    // 상위 디렉터리 이동은 전부 사라지고, 남은 이름에서 제어문자까지 제거한다.
    private String sanitizeEntryName(String fileName) {
        if (fileName == null) {
            return "file";
        }
        String normalized = fileName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        baseName = baseName.replaceAll("[\\x00-\\x1F\\x7F]", "").strip();
        if (baseName.isBlank() || baseName.equals(".") || baseName.equals("..")) {
            return "file";
        }
        return baseName;
    }

    private String uniqueEntryName(Set<String> usedNames, String fileName) {
        if (usedNames.add(fileName)) {
            return fileName;
        }
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        String candidate;
        int suffix = 2;
        do {
            candidate = base + "-" + suffix++ + extension;
        } while (!usedNames.add(candidate));
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
