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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import kgu.developers.domain.milestone.domain.MilestoneType;
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
import kgu.developers.domain.submission.exception.SubmissionArtifactTypeRequiredException;
import kgu.developers.domain.submission.exception.SubmissionInvalidArtifactTypeException;
import kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException;
import kgu.developers.domain.submission.exception.SubmissionInvalidScreensException;
import kgu.developers.domain.submission.exception.SubmissionLeaderOnlyException;
import kgu.developers.domain.submission.exception.SubmissionMilestoneTypeMismatchException;
import kgu.developers.domain.submission.exception.SubmissionPresentationImageOwnershipException;
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
        if (!isActiveStudent(milestone.getSectionId(), userId)) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 접근할 수 있습니다.");
        }

        Submission submission = submissionQueryService.getOrCreateSubmission(member.getTeamId(), milestoneId);
        return toResponse(submission);
    }

    public SubmissionResponse getSubmission(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);
        return toResponse(submission);
    }

    public SubmissionVersionListResponse getVersions(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);
        return SubmissionVersionListResponse.from(submissionVersionRepository.findAllBySubmissionId(submissionId));
    }

    public SubmissionVersionDetailResponse getVersion(Long submissionId, int version, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validateActiveTeamMembership(submission, userId);

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
        validateSubmitAllowed(submission, userId);

        if (files != null && !files.isEmpty()
                && (fileArtifactIds == null || fileArtifactIds.size() != files.size())) {
            throw new SubmissionArtifactCountMismatchException();
        }

        List<SubmissionArtifactInput> inputs = new ArrayList<>();
        if (artifacts != null) {
            for (SubmissionArtifactRequest artifact : artifacts) {
                // 컨트롤러의 @Valid는 멀티파트 List 원소까지 확실히 검증하리라 보장할 수 없어서,
                // 여기서도 직접 확인한다 — 원소 자체가 null이거나(예: "artifacts": [null]) type이
                // null이면 바로 아래에서 NPE가 나 500으로 새는 대신, 의미가 분명한 400으로
                // 떨어지게 한다(sunzx0428 PR #87 리뷰 09-03 — 원소 자체가 null인 경우가 누락됨).
                if (artifact == null || artifact.type() == null) {
                    throw new SubmissionArtifactTypeRequiredException();
                }
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
        validateActiveTeamMembership(submission, userId);
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
    // 단, 대상 마일스톤이 실제로 발표(PRESENTATION) 타입인지는 확인한다.
    public PresentationContentResponse getPresentationContent(Long submissionId, String userId) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validatePresentationMilestone(submission.getMilestoneId());
        return toPresentationContentResponse(
                submissionId, presentationContentRepository.findBySubmissionId(submissionId).orElse(null));
    }

    public PresentationContentResponse updatePresentationContent(Long submissionId, String userId, PresentationContentRequest request) {
        Submission submission = submissionQueryService.getSubmission(submissionId);
        validatePresentationMilestone(submission.getMilestoneId());
        validateActiveTeamMembership(submission, userId);
        validateNotLockedByAnother(submissionId, userId);
        validateScreenImagesOwnedBySubmission(submissionId, request.screens());
        PresentationContent content = presentationContentCommandService.upsert(
                submissionId, request.introText(), request.features(),
                stripClientProvidedImageUrls(request.screens()), request.youtubeUrl());
        return toPresentationContentResponse(submissionId, content);
    }

    public MilestonePresentationsResponse getMilestonePresentations(Long milestoneId, String userId) {
        validatePresentationMilestone(milestoneId);
        List<Submission> submissions = submissionQueryService.getSubmissionsOrderedForPresentation(milestoneId);
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        Map<Long, PresentationContent> contentBySubmissionId = presentationContentRepository
                .findAllBySubmissionIdIn(submissionIds).stream()
                .collect(Collectors.toMap(PresentationContent::getSubmissionId, c -> c));

        List<TeamPresentationResponse> contents = submissions.stream()
                .map(submission -> TeamPresentationResponse.of(
                        submission, toPresentationContentResponse(submission.getId(), contentBySubmissionId.get(submission.getId()))))
                .toList();
        return MilestonePresentationsResponse.builder().contents(contents).build();
    }

    // screens는 [{imageFileId, caption}, ...] 형식의 원본 JSON을 그대로 저장·응답하는데, imageFileId만
    // 내려주면 다른 팀 사용자는 그 이미지를 실제로 볼 방법이 없었다(presigned URL을 받는 경로가
    // 이 발표자료 조회 API 말고는 없음, sunzx0428 PR #87 리뷰 09-03). 조회할 때마다 각 화면의
    // imageFileId를 presigned URL(imageUrl)로 보강해서 내려준다 — 저장은 안 건드린다(15분 후
    // 만료되는 임시 URL이라 영구 저장하면 안 됨).
    private PresentationContentResponse toPresentationContentResponse(Long submissionId, PresentationContent content) {
        if (content == null) {
            return PresentationContentResponse.from(null, null);
        }
        return PresentationContentResponse.from(content, resolveScreenImageUrls(submissionId, content.getScreens()));
    }

    // imageFileId 소유권을 저장 시점(validateScreenImagesOwnedBySubmission)에만 확인하고 끝내면,
    // 그 뒤 뭔가의 이유로 저장된 값이 오염돼도 조회할 때마다 계속 URL이 나가버린다. 여기서도
    // isFileArtifactOfSubmission으로 다시 확인해서, 지금 시점에 소유가 아니면 URL을 만들지 않는다
    // (sunzx0428 PR #87 리뷰 09-03 2차). "imageUrl"은 항상 먼저 지우고 다시 계산한다 — 클라이언트가
    // 보낸 값이든 과거에 잘못 저장된 값이든 그대로 흘려보내지 않기 위해서다.
    private JsonNode resolveScreenImageUrls(Long submissionId, JsonNode screens) {
        if (screens == null || !screens.isArray()) {
            return screens;
        }
        ArrayNode resolved = JsonNodeFactory.instance.arrayNode();
        for (JsonNode screen : screens) {
            if (!screen.isObject()) {
                resolved.add(screen);
                continue;
            }
            ObjectNode sanitized = (ObjectNode) screen.deepCopy();
            sanitized.remove("imageUrl");
            JsonNode imageFileIdNode = screen.get("imageFileId");
            if (imageFileIdNode != null && imageFileIdNode.isIntegralNumber()
                    && isFileArtifactOfSubmission(submissionId, imageFileIdNode.asLong())) {
                fileObjectRepository.findById(imageFileIdNode.asLong())
                        .ifPresent(fileObject -> sanitized.put("imageUrl", fileStorage.presignedUrl(fileObject.getStorageKey())));
            }
            resolved.add(sanitized);
        }
        return resolved;
    }

    // presigned URL은 조회 시점에 서버가 매번 새로 만드는 값이라 저장하면 안 되는데, 클라이언트가
    // 요청 JSON에 임의의 "imageUrl"을 넣어 보내면 그게 그대로 DB에 남을 수 있었다. 저장 전에
    // 걸러낸다(sunzx0428 PR #87 리뷰 09-03 2차).
    private JsonNode stripClientProvidedImageUrls(JsonNode screens) {
        if (screens == null || !screens.isArray()) {
            return screens;
        }
        ArrayNode sanitized = JsonNodeFactory.instance.arrayNode();
        for (JsonNode screen : screens) {
            if (!screen.isObject()) {
                sanitized.add(screen);
                continue;
            }
            ObjectNode copy = (ObjectNode) screen.deepCopy();
            copy.remove("imageUrl");
            sanitized.add(copy);
        }
        return sanitized;
    }

    public void assignPresentationOrder(Long milestoneId, String professorId, PresentationOrderRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (milestone.getType() != MilestoneType.PRESENTATION) {
            throw new SubmissionMilestoneTypeMismatchException();
        }
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

    // 최종보고서 파일 제출은 팀장만 가능하다(프론트 요구사항). 그 외 마일스톤은
    // 기존대로 활성 팀원이면 누구나 제출할 수 있다.
    private void validateSubmitAllowed(Submission submission, String userId) {
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        if (milestone.getType() == MilestoneType.FINAL_REPORT) {
            validateLeader(submission, userId);
        } else {
            validateActiveTeamMembership(submission, userId);
        }
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
        if (!isActiveStudent(milestone.getSectionId(), userId)) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 접근할 수 있습니다.");
        }
    }

    private boolean isActiveStudent(Long sectionId, String userId) {
        return enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
    }

    // 발표자료 관련 API는 그 마일스톤이 실제로 PRESENTATION 타입일 때만 의미가 있다.
    // 다른 타입 마일스톤의 submissionId/milestoneId로 잘못 호출되는 것을 막는다.
    private void validatePresentationMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
        if (milestone.getType() != MilestoneType.PRESENTATION) {
            throw new SubmissionMilestoneTypeMismatchException();
        }
    }

    // screens는 형식이 자유로운 JSON([{imageFileId, caption}, ...])이라 형식 자체와 그 안의
    // imageFileId 소유권을 여기서 별도로 검증해야 한다.
    private void validateScreenImagesOwnedBySubmission(Long submissionId, JsonNode screens) {
        if (screens == null) {
            return;
        }
        if (!screens.isArray()) {
            throw new SubmissionInvalidScreensException();
        }
        for (JsonNode screen : screens) {
            if (!screen.isObject()) {
                throw new SubmissionInvalidScreensException();
            }
            JsonNode imageFileIdNode = screen.get("imageFileId");
            if (imageFileIdNode == null || imageFileIdNode.isNull()) {
                continue;
            }
            if (!imageFileIdNode.isIntegralNumber()) {
                throw new SubmissionInvalidScreensException();
            }
            if (!isFileArtifactOfSubmission(submissionId, imageFileIdNode.asLong())) {
                throw new SubmissionPresentationImageOwnershipException();
            }
        }
    }

    // imageFileId가 실제로 이 제출물 자신의 버전 이력에 FILE 아티팩트로 첨부된 적 있는 파일인지
    // 확인한다. 이 관계(Submission→SubmissionVersion→SubmissionArtifact)는 한 번 만들어지면
    // 안 바뀌므로, "업로더가 지금 이 순간 이 팀 소속인가"와 달리 팀 이동에 영향받지 않는다.
    private boolean isFileArtifactOfSubmission(Long submissionId, Long fileId) {
        return submissionVersionRepository.findAllBySubmissionId(submissionId).stream()
                .flatMap(version -> submissionArtifactRepository.findAllByVersionId(version.getId()).stream())
                .anyMatch(artifact -> artifact.getType() == ArtifactType.FILE && fileId.equals(artifact.getFileId()));
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
