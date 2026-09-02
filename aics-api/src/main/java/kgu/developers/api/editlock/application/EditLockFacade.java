package kgu.developers.api.editlock.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.submission.exception.SubmissionNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class EditLockFacade {

    private final EditLockCommandService editLockCommandService;
    private final EditLockQueryService editLockQueryService;
    private final SubmissionRepository submissionRepository;
    private final TeamMemberRepository teamMemberRepository;

    public EditLockStatusResponse getStatus(EditLockTargetType targetType, Long targetId) {
        return editLockQueryService.getActiveLock(targetType, targetId)
            .map(EditLockStatusResponse::from)
            .orElseGet(EditLockStatusResponse::unlocked);
    }

    public EditLockStatusResponse acquire(String userId, EditLockAcquireRequest request) {
        validateTargetAccess(request.targetType(), request.targetId(), userId);
        editLockCommandService.acquire(request.targetType(), request.targetId(), userId);
        return getStatus(request.targetType(), request.targetId());
    }

    public void release(EditLockTargetType targetType, Long targetId, String userId) {
        editLockCommandService.release(targetType, targetId, userId);
    }

    // 대상이 실제로 존재하고, 이 사용자가 그 대상을 편집할 권한이 있는지 확인한다.
    // targetType/targetId는 폴리모픽 참조(FK 없음)라 여기서 타입별로 갈라서 검증해야 한다.
    private void validateTargetAccess(EditLockTargetType targetType, Long targetId, String userId) {
        switch (targetType) {
            case PRESENTATION_CONTENT -> validatePresentationContentAccess(targetId, userId);
            // PROJECT(B2)는 아직 이 저장소에 도메인이 없어 검증 대상을 정할 수 없다 —
            // Project 도메인이 들어오면 팀 소속 검증을 여기 추가해야 한다.
            case PROJECT -> { }
        }
    }

    // PRESENTATION_CONTENT의 targetId는 submissionId다(발표자료는 제출 1건당 하나).
    private void validatePresentationContentAccess(Long submissionId, String userId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
        if (teamMemberRepository.findByTeamIdAndUserId(submission.getTeamId(), userId).isEmpty()) {
            throw new AccessDeniedException("그 팀 소속만 발표자료를 편집할 수 있습니다.");
        }
    }
}
