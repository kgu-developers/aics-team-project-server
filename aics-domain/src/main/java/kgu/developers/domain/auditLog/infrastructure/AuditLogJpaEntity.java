package kgu.developers.domain.auditLog.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.exception.AuditLogMetadataInvalidException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
		name = "\"audit_log\"",
		indexes = {
				@Index(name = "idx_audit_log_section", columnList = "section_id, deleted_at"),
				@Index(name = "idx_audit_log_actor", columnList = "actor_id, deleted_at")
		}
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class AuditLogJpaEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String actorId;

	@Column(nullable = false)
	private Long sectionId;

	@Column(nullable = false, length = 50)
	private String eventType;

	@Column(nullable = false)
	private Long targetType;

	@Column(nullable = false)
	private Long targetId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String metadata;

	public AuditLog toDomain() {
		return AuditLog.builder()
				.id(id)
				.actorId(actorId)
				.sectionId(sectionId)
				.eventType(eventType)
				.targetType(targetType)
				.targetId(targetId)
				.metadata(metadata != null && !metadata.isBlank()
						? JsonConverter.parse(metadata, AuditLogMetadataInvalidException::new)
						: JsonConverter.parse("{}"))
				.createdAt(getCreatedAt())
				.updatedAt(getUpdatedAt())
				.deletedAt(getDeletedAt())
				.build();
	}

	public static AuditLogJpaEntity toEntity(AuditLog auditLog) {
		AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
				.id(auditLog.getId())
				.actorId(auditLog.getActorId())
				.sectionId(auditLog.getSectionId())
				.eventType(auditLog.getEventType())
				.targetType(auditLog.getTargetType())
				.targetId(auditLog.getTargetId())
				.metadata(auditLog.getMetadata() != null ? auditLog.getMetadata().toString() : "{}")
				.build();
		entity.createdAt = auditLog.getCreatedAt();
		entity.setDeletedAt(auditLog.getDeletedAt());
		return entity;
	}
}
