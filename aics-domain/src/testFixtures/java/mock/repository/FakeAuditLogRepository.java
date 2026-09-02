package mock.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;
import kgu.developers.domain.auditLog.domain.TargetType;
import kgu.developers.domain.auditLog.exception.AuditLogNotFoundException;

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
	public Page<AuditLog> findAllBySectionId(Long sectionId, Pageable pageable) {
		if (sectionId == null) {
			return Page.empty(pageable);
		}
		List<AuditLog> filtered = store.values().stream()
				.filter(log -> log.getDeletedAt() == null)
				.filter(log -> sectionId.equals(log.getSectionId()))
				.toList();
		return paginate(applySort(filtered, pageable.getSort()), pageable);
	}

	@Override
	public Page<AuditLog> findAllByActorId(String actorId, Pageable pageable) {
		if (actorId == null || actorId.isBlank()) {
			return Page.empty(pageable);
		}
		List<AuditLog> filtered = store.values().stream()
				.filter(log -> log.getDeletedAt() == null)
				.filter(log -> actorId.equals(log.getActorId()))
				.toList();
		return paginate(applySort(filtered, pageable.getSort()), pageable);
	}

	@Override
	public Page<AuditLog> findAllByEventType(String eventType, Pageable pageable) {
		if (eventType == null || eventType.isBlank()) {
			return Page.empty(pageable);
		}
		List<AuditLog> filtered = store.values().stream()
				.filter(log -> log.getDeletedAt() == null)
				.filter(log -> eventType.equals(log.getEventType()))
				.toList();
		return paginate(applySort(filtered, pageable.getSort()), pageable);
	}

	@Override
	public Page<AuditLog> findAllByTeam(Long sectionId, Long teamId, Pageable pageable) {
		if (sectionId == null || teamId == null) {
			return Page.empty(pageable);
		}
		List<AuditLog> filtered = activeLogs()
				.filter(log -> sectionId.equals(log.getSectionId()))
				.filter(log -> TargetType.TEAM == log.getTargetType())
				.filter(log -> teamId.equals(log.getTargetId()))
				.toList();
		return paginate(applySort(filtered, pageable.getSort()), pageable);
	}

	@Override
	public List<AuditLog> findAllByTeamAndActorIdIn(Long sectionId, Long teamId, List<String> actorIds) {
		if (sectionId == null || teamId == null || actorIds == null || actorIds.isEmpty()) {
			return List.of();
		}
		return activeLogs()
				.filter(log -> sectionId.equals(log.getSectionId()))
				.filter(log -> TargetType.TEAM == log.getTargetType())
				.filter(log -> teamId.equals(log.getTargetId()))
				.filter(log -> actorIds.contains(log.getActorId()))
				.sorted(newestFirst())
				.toList();
	}

	private List<AuditLog> applySort(List<AuditLog> logs, Sort sort) {
		if (sort.isUnsorted()) {
			return logs;
		}
		Comparator<AuditLog> comparator = null;
		for (Sort.Order order : sort) {
			Comparator<AuditLog> propertyComparator = comparatorFor(order.getProperty());
			if (order.isDescending()) {
				propertyComparator = propertyComparator.reversed();
			}
			comparator = comparator == null
					? propertyComparator
					: comparator.thenComparing(propertyComparator);
		}
		return logs.stream().sorted(comparator).toList();
	}

	private Comparator<AuditLog> comparatorFor(String property) {
		return switch (property) {
			case "id" -> Comparator.comparing(AuditLog::getId, Comparator.nullsLast(Comparator.naturalOrder()));
			case "createdAt" -> Comparator.comparing(
					AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
			case "actorId" -> Comparator.comparing(
					AuditLog::getActorId, Comparator.nullsLast(Comparator.naturalOrder()));
			case "eventType" -> Comparator.comparing(
					AuditLog::getEventType, Comparator.nullsLast(Comparator.naturalOrder()));
			default -> throw new IllegalArgumentException("지원하지 않는 AuditLog 정렬 속성입니다: " + property);
		};
	}

	private java.util.stream.Stream<AuditLog> activeLogs() {
		return store.values().stream().filter(log -> log.getDeletedAt() == null);
	}

	private Comparator<AuditLog> newestFirst() {
		return Comparator
				.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(AuditLog::getId, Comparator.nullsLast(Comparator.reverseOrder()));
	}

	private Page<AuditLog> paginate(List<AuditLog> items, Pageable pageable) {
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), items.size());
		if (start >= items.size()) {
			return new PageImpl<>(List.of(), pageable, items.size());
		}
		return new PageImpl<>(items.subList(start, end), pageable, items.size());
	}

	@Override
	public void deleteById(Long id) {
		findById(id)
				.orElseThrow(AuditLogNotFoundException::new)
				.delete();
	}

	public void clear() {
		store.clear();
	}
}
