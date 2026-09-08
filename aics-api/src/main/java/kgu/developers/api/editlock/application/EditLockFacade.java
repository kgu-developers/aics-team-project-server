package kgu.developers.api.editlock.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockUnsupportedTargetException;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class EditLockFacade {

    private final EditLockCommandService editLockCommandService;
    private final EditLockQueryService editLockQueryService;

    // acquire()와 같은 대상 접근 검증을 거친다 — 검증 없이 조회를 허용하면 다른 분반·팀
    // 사용자도 lockedBy(학번)를 알아낼 수 있었다(sunzx0428 PR #87 리뷰 09-03).
    public EditLockStatusResponse getStatus(EditLockTargetType targetType, Long targetId, String sectionKey, String userId) {
        validateTargetAccess(targetType, targetId, userId);
        return getStatusWithoutAccessCheck(targetType, targetId, sectionKey);
    }

    private EditLockStatusResponse getStatusWithoutAccessCheck(EditLockTargetType targetType, Long targetId, String sectionKey) {
        return editLockQueryService.getActiveLock(targetType, targetId, sectionKey)
            .map(EditLockStatusResponse::from)
            .orElseGet(EditLockStatusResponse::unlocked);
    }

    public EditLockStatusResponse acquire(String userId, EditLockAcquireRequest request) {
        validateTargetAccess(request.targetType(), request.targetId(), userId);
        editLockCommandService.acquire(request.targetType(), request.targetId(), request.sectionKey(), userId);
        return getStatusWithoutAccessCheck(request.targetType(), request.targetId(), request.sectionKey());
    }

    public void release(EditLockTargetType targetType, Long targetId, String sectionKey, String userId) {
        editLockCommandService.release(targetType, targetId, sectionKey, userId);
    }

    // 대상이 실제로 존재하고, 이 사용자가 그 대상을 편집할 권한이 있는지 확인한다.
    // targetType/targetId는 폴리모픽 참조(FK 없음)라 여기서 타입별로 갈라서 검증해야 한다.
    private void validateTargetAccess(EditLockTargetType targetType, Long targetId, String userId) {
        switch (targetType) {
            // PROJECT(B2)는 아직 이 저장소에 도메인이 없어 검증 대상을 정할 수 없다 — 검증 없이
            // 통과시키면 아무 인증 사용자나 임의 대상을 잠글 수 있게 되므로, Project 도메인이
            // 들어오기 전까지는 아예 지원하지 않는 대상으로 명시적으로 거부한다.
            case PROJECT -> throw new EditLockUnsupportedTargetException();
        }
    }
}
