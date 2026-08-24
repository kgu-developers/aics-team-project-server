package kgu.developers.domain.editlock.infrastructure;

import java.util.Optional;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEditLockRepository extends JpaRepository<EditLockJpaEntity, Long> {

    Optional<EditLockJpaEntity> findByTargetTypeAndTargetId(EditLockTargetType targetType, Long targetId);
}
