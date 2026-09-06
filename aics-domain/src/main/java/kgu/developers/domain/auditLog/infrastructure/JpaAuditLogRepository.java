package kgu.developers.domain.auditLog.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kgu.developers.domain.auditLog.domain.AuditLogEventType;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {

	Optional<AuditLogJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	Page<AuditLogJpaEntity> findAllBySectionIdAndDeletedAtIsNull(Long sectionId, Pageable pageable);

	Page<AuditLogJpaEntity> findAllByActorIdAndDeletedAtIsNull(String actorId, Pageable pageable);

	Page<AuditLogJpaEntity> findAllByEventTypeAndDeletedAtIsNull(AuditLogEventType eventType, Pageable pageable);

	Page<AuditLogJpaEntity> findAllBySectionIdAndTargetTypeAndTargetIdAndDeletedAtIsNull(
			Long sectionId, Long targetType, Long targetId, Pageable pageable);

	List<AuditLogJpaEntity> findAllBySectionIdAndTargetTypeAndTargetIdAndActorIdInAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
			Long sectionId, Long targetType, Long targetId, List<String> actorIds);
}
