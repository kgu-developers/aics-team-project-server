package kgu.developers.domain.editlock.application.command;

import java.time.LocalDateTime;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockRepository;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EditLockCommandService {

    private final EditLockRepository editLockRepository;

    public void acquire(EditLockTargetType targetType, Long targetId, String userId) {
        LocalDateTime now = LocalDateTime.now();
        editLockRepository.findByTargetTypeAndTargetId(targetType, targetId)
            .ifPresentOrElse(
                existing -> renewOrTakeOver(existing, userId, now),
                () -> editLockRepository.save(EditLock.create(targetType, targetId, userId, now))
            );
    }

    // 본인 소유거나 만료됐으면 갱신(하트비트/인수), 타인이 살아있게 쥐고 있으면 409.
    private void renewOrTakeOver(EditLock existing, String userId, LocalDateTime now) {
        if (!existing.isOwnedBy(userId) && !existing.isExpired(now)) {
            throw new EditLockConflictException();
        }
        existing.renew(userId, now);
        editLockRepository.save(existing);
    }

    // 본인 소유 잠금만 해제. 없거나 타인 소유면 조용히 아무 일도 하지 않는다(DELETE는 멱등).
    public void release(EditLockTargetType targetType, Long targetId, String userId) {
        editLockRepository.findByTargetTypeAndTargetId(targetType, targetId)
            .filter(editLock -> editLock.isOwnedBy(userId))
            .ifPresent(editLock -> editLockRepository.deleteById(editLock.getId()));
    }
}
