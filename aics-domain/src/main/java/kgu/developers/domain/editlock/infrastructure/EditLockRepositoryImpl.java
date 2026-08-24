package kgu.developers.domain.editlock.infrastructure;

import java.util.Optional;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockRepository;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EditLockRepositoryImpl implements EditLockRepository {

    private final JpaEditLockRepository jpaEditLockRepository;

    @Override
    public EditLock save(EditLock editLock) {
        try {
            return jpaEditLockRepository.saveAndFlush(EditLockJpaEntity.toEntity(editLock)).toDomain();
        } catch (DataIntegrityViolationException e) {
            // 신규 획득 시 동시에 둘 이상이 시도하면 uk_edit_lock_target 유니크 제약이 하나만 통과시킨다.
            throw new EditLockConflictException();
        }
    }

    @Override
    public Optional<EditLock> findByTargetTypeAndTargetId(EditLockTargetType targetType, Long targetId) {
        return jpaEditLockRepository.findByTargetTypeAndTargetId(targetType, targetId)
            .map(EditLockJpaEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaEditLockRepository.deleteById(id);
    }
}
