package kgu.developers.domain.auditLog.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {

	Optional<AuditLogJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	Page<AuditLogJpaEntity> findAllBySectionIdAndDeletedAtIsNull(Long sectionId, Pageable pageable);

	Page<AuditLogJpaEntity> findAllByActorIdAndDeletedAtIsNull(String actorId, Pageable pageable);

	Page<AuditLogJpaEntity> findAllByEventTypeAndDeletedAtIsNull(String eventType, Pageable pageable);
}
