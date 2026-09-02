package kgu.developers.domain.auditLog.domain;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository {

	AuditLog save(AuditLog auditLog);

	Optional<AuditLog> findById(Long id);

	Page<AuditLog> findAllBySectionId(Long sectionId, Pageable pageable);

	Page<AuditLog> findAllByActorId(String actorId, Pageable pageable);

	Page<AuditLog> findAllByEventType(String eventType, Pageable pageable);

	void deleteById(Long id);
}
