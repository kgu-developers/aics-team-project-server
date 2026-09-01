package kgu.developers.domain.auditLog.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository {

	AuditLog save(AuditLog auditLog);

	Optional<AuditLog> findById(Long id);

	Page<AuditLog> findAllBySectionId(Long sectionId, Pageable pageable);

	Page<AuditLog> findAllByActorId(String actorId, Pageable pageable);

	Page<AuditLog> findAllByEventType(String eventType, Pageable pageable);

	Page<AuditLog> findAllByTeam(Long sectionId, Long teamId, Pageable pageable);

	List<AuditLog> findAllBySectionIdAndActorIdIn(Long sectionId, List<String> actorIds);

	void deleteById(Long id);
}
