package kgu.developers.domain.editlock.infrastructure;

import java.util.Optional;
import kgu.developers.common.exception.OptimisticLocks;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockRepository;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Slf4j
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
            // KD3-238: 유니크 충돌인지 NOT NULL 등 기타 DB 제약조건 위반인지 추적할 수 있도록 상세 로그를 남긴다.
            log.warn("EditLock 저장 실패 (DB 제약조건 위반): {}", e.getMostSpecificCause().getMessage(), e);
            throw new EditLockConflictException();
        }
    }

    @Override
    public Optional<EditLock> findByTargetTypeAndTargetIdAndSectionKey(
        EditLockTargetType targetType, Long targetId, String sectionKey
    ) {
        return jpaEditLockRepository.findByTargetTypeAndTargetIdAndSectionKey(targetType, targetId, sectionKey)
            .map(EditLockJpaEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaEditLockRepository.deleteById(id);
    }
}
