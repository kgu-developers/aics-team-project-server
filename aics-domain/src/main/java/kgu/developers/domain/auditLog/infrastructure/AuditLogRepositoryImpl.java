package kgu.developers.domain.auditLog.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;
import kgu.developers.domain.auditLog.exception.AuditLogNotFoundException;
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
	public Page<AuditLog> findAllBySectionId(Long sectionId, Pageable pageable) {
		if (sectionId == null) {
			return Page.empty(pageable);
		}
		return jpaAuditLogRepository.findAllBySectionIdAndDeletedAtIsNull(sectionId, pageable)
				.map(AuditLogJpaEntity::toDomain);
	}

	@Override
	public Page<AuditLog> findAllByActorId(String actorId, Pageable pageable) {
		if (actorId == null || actorId.isBlank()) {
			return Page.empty(pageable);
		}
		return jpaAuditLogRepository.findAllByActorIdAndDeletedAtIsNull(actorId, pageable)
				.map(AuditLogJpaEntity::toDomain);
	}

	@Override
	public Page<AuditLog> findAllByEventType(String eventType, Pageable pageable) {
		if (eventType == null || eventType.isBlank()) {
			return Page.empty(pageable);
		}
		return jpaAuditLogRepository.findAllByEventTypeAndDeletedAtIsNull(eventType, pageable)
				.map(AuditLogJpaEntity::toDomain);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		jpaAuditLogRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(AuditLogNotFoundException::new)
				.delete();
	}
}
