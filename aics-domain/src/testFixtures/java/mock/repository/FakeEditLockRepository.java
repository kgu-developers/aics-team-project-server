package mock.repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockRepository;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;

public class FakeEditLockRepository implements EditLockRepository {

    private final Map<Long, EditLock> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public EditLock save(EditLock editLock) {
        if (editLock.getId() == null) {
            boolean alreadyExists = store.values().stream()
                .anyMatch(existing -> existing.getTargetType() == editLock.getTargetType()
                    && existing.getTargetId().equals(editLock.getTargetId()));
            if (alreadyExists) {
                // 실제 구현은 uk_edit_lock_target 유니크 제약 위반으로 이걸 검출한다.
                throw new EditLockConflictException();
            }
        }

        Long id = editLock.getId() != null ? editLock.getId() : sequence.incrementAndGet();

        EditLock saved = EditLock.builder()
            .id(id)
            .targetType(editLock.getTargetType())
            .targetId(editLock.getTargetId())
            .lockedBy(editLock.getLockedBy())
            .lockedAt(editLock.getLockedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<EditLock> findByTargetTypeAndTargetId(EditLockTargetType targetType, Long targetId) {
        return store.values().stream()
            .filter(editLock -> editLock.getTargetType() == targetType)
            .filter(editLock -> editLock.getTargetId().equals(targetId))
            .findFirst();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    /** 테스트에서 만료 시나리오를 만들기 위한 헬퍼. 실제 Repository 인터페이스엔 없다. */
    public void forceLockedAt(Long targetIdOfLock, LocalDateTime lockedAt) {
        EditLock existing = store.get(targetIdOfLock);
        existing.renew(existing.getLockedBy(), lockedAt);
    }
}
