package kgu.developers.api.submission.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.api.submission.presentation.request.PresentationContentRequest;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionArtifactRequest;
import kgu.developers.api.submission.presentation.request.SubmissionMemberConfirmationRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.PresentationContentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionArtifactResponse;
import kgu.developers.api.submission.presentation.response.SubmissionMemberConfirmationListResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionDetailResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionListResponse;
import kgu.developers.api.submission.presentation.response.TeamPresentationResponse;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.fileobject.exception.FileObjectNotFoundException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.presentationcontent.application.command.PresentationContentCommandService;
import kgu.developers.domain.presentationcontent.domain.PresentationContent;
import kgu.developers.domain.presentationcontent.domain.PresentationContentRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.command.SubmissionArtifactInput;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmationRepository;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.submission.exception.SubmissionAccessDeniedException;
import kgu.developers.domain.submission.exception.SubmissionLeaderOnlyException;
import kgu.developers.domain.submission.exception.SubmissionVersionNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class SubmissionFacade {
    private final SubmissionCommandService submissionCommandService;
    private final SubmissionQueryService submissionQueryService;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final SubmissionArtifactRepository submissionArtifactRepository;
    private final SubmissionMemberConfirmationRepository submissionMemberConfirmationRepository;
    private final PresentationContentRepository presentationContentRepository;
    private final PresentationContentCommandService presentationContentCommandService;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final MilestoneRepository milestoneRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SectionQueryService sectionQueryService;

    public SubmissionResponse getMyTeamSubmission(Long milestoneId, String userId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        TeamMember member = teamMemberRepository.findActiveBySectionIdAndUserId(milestone.getSectionId(), userId)
                .orElseThrow(() -> new AccessDeniedException("그 분반의 팀 소속만 접근할 수 있습니다."));

        Submission submission = submissionQueryService.getOrCreateSubmission(member.getTeamId(), milestoneId);
        return toResponse(submission);
    }

    public SubmissionResponse getSubmission(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);
        return toResponse(submission);
    }

    public SubmissionVersionListResponse getVersions(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);
        return SubmissionVersionListResponse.from(submissionVersionRepository.findAllBySubmissionId(submissionId));
    }

    public SubmissionVersionDetailResponse getVersion(Long submissionId, int version, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);

        SubmissionVersion submissionVersion = submissionVersionRepository
                .findBySubmissionIdAndVersion(submissionId, version)
                .orElseThrow(SubmissionVersionNotFoundException::new);

        List<SubmissionArtifactResponse> artifacts = submissionArtifactRepository
                .findAllByVersionId(submissionVersion.getId()).stream()
                .map(this::toArtifactResponse)
                .toList();

        return SubmissionVersionDetailResponse.of(submissionVersion, artifacts);
    }

    public SubmissionResponse submitVersion(
            Long submissionId,
            String userId,
            String description,
            String changeNote,
            List<SubmissionArtifactRequest> artifacts,
            List<Long> fileArtifactIds,
            List<MultipartFile> files
    ) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);

        List<SubmissionArtifactInput> inputs = new ArrayList<>();
        if (artifacts != null) {
            artifacts.forEach(artifact -> inputs.add(new SubmissionArtifactInput(
                    artifact.requiredArtifactId(), artifact.type(), null, artifact.url(), artifact.content())));
        }
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                Long requiredArtifactId = (fileArtifactIds != null && i < fileArtifactIds.size())
                        ? fileArtifactIds.get(i) : null;
                inputs.add(new SubmissionArtifactInput(requiredArtifactId, ArtifactType.FILE, files.get(i), null, null));
            }
        }

        submissionCommandService.submitVersion(submissionId, userId, description, changeNote, inputs);
        return toResponse(submissionQueryService.getSubmission(submissionId));
    }

    public SubmissionMemberConfirmationListResponse getMemberConfirmations(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);
        return SubmissionMemberConfirmationListResponse.from(
                submissionMemberConfirmationRepository.findAllBySubmissionId(submissionId));
    }

    public void confirmAsMember(Long submissionId, String userId, SubmissionMemberConfirmationRequest request) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);
        submissionCommandService.confirmAsMember(
                submissionId, userId, request.confirmedFinalReport(), request.confirmedArtifacts(), request.oneLineReview());
    }

    public SubmissionResponse completeSubmission(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateLeader(submission.getTeamId(), userId);
        submissionCommandService.completeSubmission(submissionId);
        return toResponse(submissionQueryService.getSubmission(submissionId));
    }

    public SubmissionResponse reopenSubmission(Long submissionId, String professorId, SubmissionReopenRequest request) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(milestone.getSectionId(), professorId)) {
            throw new SubmissionAccessDeniedException();
        }
        submissionCommandService.reopenSubmission(submissionId, professorId, request.revisionDueAt());
        return toResponse(submissionQueryService.getSubmission(submissionId));
    }

    // 발표 공개자료는 다른 팀도 상시 열람 가능(PRD 그대로) — 로그인만 하면 되고 팀 소속 검증은 안 한다.
    public PresentationContentResponse getPresentationContent(Long submissionId, String userId) {
        submissionQueryService.getSubmission(submissionId);
        return PresentationContentResponse.from(presentationContentRepository.findBySubmissionId(submissionId).orElse(null));
    }

    public PresentationContentResponse updatePresentationContent(Long submissionId, String userId, PresentationContentRequest request) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateTeamMembership(submission.getTeamId(), userId);
        PresentationContent content = presentationContentCommandService.upsert(
                submissionId, request.introText(), request.features(), request.screens(), request.youtubeUrl());
        return PresentationContentResponse.from(content);
    }

    public MilestonePresentationsResponse getMilestonePresentations(Long milestoneId, String userId) {
        List<Submission> submissions = submissionQueryService.getSubmissionsOrderedForPresentation(milestoneId);
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        Map<Long, PresentationContent> contentBySubmissionId = presentationContentRepository
                .findAllBySubmissionIdIn(submissionIds).stream()
                .collect(Collectors.toMap(PresentationContent::getSubmissionId, c -> c));

        List<TeamPresentationResponse> contents = submissions.stream()
                .map(submission -> TeamPresentationResponse.of(submission, contentBySubmissionId.get(submission.getId())))
                .toList();
        return MilestonePresentationsResponse.builder().contents(contents).build();
    }

    public void assignPresentationOrder(Long milestoneId, String professorId, PresentationOrderRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(milestone.getSectionId(), professorId)) {
            throw new SubmissionAccessDeniedException();
        }
        Map<Long, Integer> orderByTeamId = request.teamOrders().stream()
                .collect(Collectors.toMap(PresentationOrderRequest.TeamOrder::teamId, PresentationOrderRequest.TeamOrder::order));
        submissionCommandService.assignPresentationOrders(milestoneId, orderByTeamId);
    }

    private void validateLeader(Long teamId, String userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(SubmissionAccessDeniedException::new);
        if (!member.isLeader()) {
            throw new SubmissionLeaderOnlyException();
        }
    }

    private SubmissionResponse toResponse(Submission submission) {
        return SubmissionResponse.of(
                submission,
                submissionQueryService.canSubmitNow(submission),
                submissionQueryService.hasPendingReview(submission)
        );
    }

    private SubmissionArtifactResponse toArtifactResponse(SubmissionArtifact artifact) {
        if (artifact.getType() != ArtifactType.FILE) {
            return SubmissionArtifactResponse.of(artifact);
        }
        FileObject fileObject = fileObjectRepository.findById(artifact.getFileId())
                .orElseThrow(FileObjectNotFoundException::new);
        String downloadUrl = fileStorage.presignedUrl(fileObject.getStorageKey());
        return SubmissionArtifactResponse.ofFile(artifact, fileObject, downloadUrl);
    }

    private void validateTeamMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("그 팀에 소속된 사용자만 접근할 수 있습니다.");
        }
    }
}
