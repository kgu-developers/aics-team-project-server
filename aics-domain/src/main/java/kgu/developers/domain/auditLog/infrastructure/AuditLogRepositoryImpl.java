package kgu.developers.domain.auditLog.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

	private final JpaAuditLogRepository jpaAuditLogRepository;

	@Override
	public AuditLog save(AuditLog auditLog) {
		AuditLogJpaEntity entity = AuditLogJpaEntity.toEntity(auditLog);
		return jpaAuditLogRepository.save(entity).toDomain();
	}

	@Override
	public Optional<AuditLog> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return jpaAuditLogRepository.findByIdAndDeletedAtIsNull(id)
				.map(AuditLogJpaEntity::toDomain);
	}

	@Override
	public List<AuditLog> findAllBySectionId(Long sectionId) {
		if (sectionId == null) {
			return List.of();
		}
		return jpaAuditLogRepository.findAllBySectionIdAndDeletedAtIsNullOrderByCreatedAtDesc(sectionId)
				.stream()
				.map(AuditLogJpaEntity::toDomain)
				.toList();
	}

	@Override
	public List<AuditLog> findAllByActorId(String actorId) {
		if (actorId == null || actorId.isBlank()) {
			return List.of();
		}
		return jpaAuditLogRepository.findAllByActorIdAndDeletedAtIsNullOrderByCreatedAtDesc(actorId)
				.stream()
				.map(AuditLogJpaEntity::toDomain)
				.toList();
	}

	@Override
	public List<AuditLog> findAllByEventType(String eventType) {
		if (eventType == null || eventType.isBlank()) {
			return List.of();
		}
		return jpaAuditLogRepository.findAllByEventTypeAndDeletedAtIsNullOrderByCreatedAtDesc(eventType)
				.stream()
				.map(AuditLogJpaEntity::toDomain)
				.toList();
	}

	@Override
	public void deleteById(Long id) {
		if (id != null) {
			jpaAuditLogRepository.deleteById(id);
		}
	}
}
