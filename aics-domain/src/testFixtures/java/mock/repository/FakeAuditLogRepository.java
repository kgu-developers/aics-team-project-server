package mock.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;

public class FakeAuditLogRepository implements AuditLogRepository {

	private final Map<Long, AuditLog> store = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong(0);

	@Override
	public AuditLog save(AuditLog auditLog) {
		Long id = auditLog.getId() != null ? auditLog.getId() : sequence.incrementAndGet();

		AuditLog saved = AuditLog.builder()
				.id(id)
				.actorId(auditLog.getActorId())
				.sectionId(auditLog.getSectionId())
				.eventType(auditLog.getEventType())
				.targetType(auditLog.getTargetType())
				.targetId(auditLog.getTargetId())
				.metadata(auditLog.getMetadata())
				.createdAt(auditLog.getCreatedAt())
				.updatedAt(auditLog.getUpdatedAt())
				.deletedAt(auditLog.getDeletedAt())
				.build();

		store.put(id, saved);
		return saved;
	}

	@Override
	public Optional<AuditLog> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(store.get(id))
				.filter(log -> log.getDeletedAt() == null);
	}

	@Override
	public List<AuditLog> findAllBySectionId(Long sectionId) {
		if (sectionId == null) {
			return List.of();
		}
		return store.values().stream()
				.filter(log -> log.getDeletedAt() == null)
				.filter(log -> sectionId.equals(log.getSectionId()))
				.sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	@Override
	public List<AuditLog> findAllByActorId(String actorId) {
		if (actorId == null || actorId.isBlank()) {
			return List.of();
		}
		return store.values().stream()
				.filter(log -> log.getDeletedAt() == null)
				.filter(log -> actorId.equals(log.getActorId()))
				.sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	@Override
	public List<AuditLog> findAllByEventType(String eventType) {
		if (eventType == null || eventType.isBlank()) {
			return List.of();
		}
		return store.values().stream()
				.filter(log -> log.getDeletedAt() == null)
				.filter(log -> eventType.equals(log.getEventType()))
				.sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	@Override
	public void deleteById(Long id) {
		if (id != null) {
			store.remove(id);
		}
	}

	public void clear() {
		store.clear();
	}
}
