package kgu.developers.domain.editlock.domain;

import java.util.Optional;

public interface EditLockRepository {

    EditLock save(EditLock editLock);

    Optional<EditLock> findByTargetTypeAndTargetId(EditLockTargetType targetType, Long targetId);

    void deleteById(Long id);
}
