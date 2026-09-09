package kgu.developers.domain.editlock.domain;

import java.util.Optional;

public interface EditLockRepository {

    EditLock save(EditLock editLock);

    Optional<EditLock> findByTargetTypeAndTargetIdAndSectionKey(
        EditLockTargetType targetType, Long targetId, String sectionKey
    );

    void deleteById(Long id);
}
