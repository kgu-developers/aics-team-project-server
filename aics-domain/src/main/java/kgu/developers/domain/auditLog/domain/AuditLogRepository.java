package kgu.developers.domain.auditLog.domain;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository {

	AuditLog save(AuditLog auditLog);

	Optional<AuditLog> findById(Long id);

	List<AuditLog> findAllBySectionId(Long sectionId);

	List<AuditLog> findAllByActorId(String actorId);

	List<AuditLog> findAllByEventType(String eventType);

	void deleteById(Long id);
}
