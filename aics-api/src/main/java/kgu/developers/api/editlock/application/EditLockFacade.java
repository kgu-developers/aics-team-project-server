package kgu.developers.api.editlock.application;

import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class EditLockFacade {

    private final EditLockCommandService editLockCommandService;
    private final EditLockQueryService editLockQueryService;

    public EditLockStatusResponse getStatus(EditLockTargetType targetType, Long targetId) {
        return editLockQueryService.getActiveLock(targetType, targetId)
            .map(EditLockStatusResponse::from)
            .orElseGet(EditLockStatusResponse::unlocked);
    }

    public EditLockStatusResponse acquire(String userId, EditLockAcquireRequest request) {
        editLockCommandService.acquire(request.targetType(), request.targetId(), userId);
        return getStatus(request.targetType(), request.targetId());
    }

    public void release(EditLockTargetType targetType, Long targetId, String userId) {
        editLockCommandService.release(targetType, targetId, userId);
    }
}
