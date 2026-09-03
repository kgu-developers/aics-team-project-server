package kgu.developers.domain.editlock.infrastructure;

import java.util.Optional;
import kgu.developers.common.exception.OptimisticLocks;
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
            // 신규 획득 시 동시에 둘 이상이 시도하면 uk_edit_lock_target 유니크 제약이 하나만
            // 통과시키고(DataIntegrityViolationException), 기존 잠금을 갱신/인수하는 경우엔
            // @Version 낙관적 락이 동시 갱신을 잡아낸다(OptimisticLockingFailureException) —
            // 둘 다 "이미 누가 가져갔다"는 같은 의미라 동일한 예외로 변환한다.
            return OptimisticLocks.translate(
                () -> jpaEditLockRepository.saveAndFlush(EditLockJpaEntity.toEntity(editLock)).toDomain(),
                EditLockConflictException::new
            );
        } catch (DataIntegrityViolationException e) {
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
