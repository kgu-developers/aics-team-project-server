package kgu.developers.api.submission.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
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
import kgu.developers.domain.submission.exception.SubmissionArtifactCountMismatchException;
import kgu.developers.domain.submission.exception.SubmissionInvalidArtifactTypeException;
import kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException;
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
    private final EnrollmentRepository enrollmentRepository;
    private final EditLockQueryService editLockQueryService;
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
        validateActiveTeamMembership(submission, userId);

        if (files != null && !files.isEmpty()
                && (fileArtifactIds == null || fileArtifactIds.size() != files.size())) {
            throw new SubmissionArtifactCountMismatchException();
        }

        List<SubmissionArtifactInput> inputs = new ArrayList<>();
        if (artifacts != null) {
            for (SubmissionArtifactRequest artifact : artifacts) {
                if (artifact.type() == ArtifactType.FILE) {
                    throw new SubmissionInvalidArtifactTypeException();
                }
                inputs.add(new SubmissionArtifactInput(
                        artifact.requiredArtifactId(), artifact.type(), null, artifact.url(), artifact.content()));
            }
        }
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                inputs.add(new SubmissionArtifactInput(fileArtifactIds.get(i), ArtifactType.FILE, files.get(i), null, null));
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
        validateActiveTeamMembership(submission, userId);
        submissionCommandService.confirmAsMember(
                submissionId, userId, request.confirmedFinalReport(), request.confirmedArtifacts(), request.oneLineReview());
    }

    public SubmissionResponse completeSubmission(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateLeader(submission, userId);
        submissionCommandService.completeSubmission(submissionId, userId);
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
        validateActiveTeamMembership(submission, userId);
        validateNotLockedByAnother(submissionId, userId);
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
        // Collectors.toMap은 키가 중복되면 IllegalStateException(500)을 던지므로, 여기서
        // 직접 넣으면서 중복 teamId를 400으로 미리 걸러낸다.
        Map<Long, Integer> orderByTeamId = new LinkedHashMap<>();
        for (PresentationOrderRequest.TeamOrder teamOrder : request.teamOrders()) {
            if (orderByTeamId.put(teamOrder.teamId(), teamOrder.order()) != null) {
                throw new SubmissionInvalidPresentationOrderException();
            }
        }
        submissionCommandService.assignPresentationOrders(milestoneId, orderByTeamId);
    }

    // 탈퇴했거나 조교로 전환된 기존 팀장이 계속 완료 처리할 수 있던 구멍을 막기 위해,
    // 팀장 여부뿐 아니라 지금도 그 분반의 활성 학생인지까지 같이 확인한다.
    private void validateLeader(Submission submission, String userId) {
        validateActiveTeamMembership(submission, userId);
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(submission.getTeamId(), userId)
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

    // 쓰기 경로(제출/확인/발표자료 수정) 전용 — 팀원 행이 남아있는 것만으로는 부족하고,
    // 지금 이 분반에 "활성 학생"으로 등록돼 있어야 한다. 탈퇴했거나 조교로 역할이 바뀐 뒤에도
    // TeamMember 행만 안 지워지면 계속 쓸 수 있던 구멍을 막는다.
    private void validateActiveTeamMembership(Submission submission, String userId) {
        validateTeamMembership(submission.getTeamId(), userId);
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        boolean activeStudent = enrollmentRepository.findBySectionIdAndUserId(milestone.getSectionId(), userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
        if (!activeStudent) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 접근할 수 있습니다.");
        }
    }

    // 다른 사람이 지금 이 발표자료를 편집 중(EditLock 보유)이면 덮어쓰지 못하게 막는다.
    // 잠금을 아무도 안 잡았으면 그대로 허용 — 잠금 자체는 여전히 선택 사항이고, 이건
    // "잡은 잠금이 있으면 그 소유자만 쓸 수 있다"는 최소한의 강제만 건다.
    private void validateNotLockedByAnother(Long submissionId, String userId) {
        editLockQueryService.getActiveLock(EditLockTargetType.PRESENTATION_CONTENT, submissionId)
                .filter(lock -> !lock.isOwnedBy(userId))
                .ifPresent(lock -> {
                    throw new SubmissionAccessDeniedException();
                });
    }
}
