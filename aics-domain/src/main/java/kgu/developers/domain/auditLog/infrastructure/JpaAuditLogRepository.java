package kgu.developers.domain.auditLog.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {

	Optional<AuditLogJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	List<AuditLogJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long sectionId);

	List<AuditLogJpaEntity> findAllByActorIdAndDeletedAtIsNullOrderByCreatedAtDesc(String actorId);

	List<AuditLogJpaEntity> findAllByEventTypeAndDeletedAtIsNullOrderByCreatedAtDesc(String eventType);
}
