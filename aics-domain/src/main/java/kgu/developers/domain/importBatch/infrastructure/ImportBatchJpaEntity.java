package kgu.developers.domain.importBatch.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.importBatch.exception.ImportBatchPayloadInvalidException;
import kgu.developers.domain.importBatch.domain.Status;
import kgu.developers.domain.importBatch.domain.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
		name = "\"import_batch\"",
		indexes = @Index(name = "idx_import_batch_section", columnList = "section_id, deleted_at")
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ImportBatchJpaEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Version
	private Long version;

	@Column(nullable = false, length = 20)
	private String uploadedBy;

	@Column(nullable = false)
	private Long sectionId;

	@Column(nullable = false, length = 16)
	@Enumerated(STRING)
	private Type type;

	@Column(nullable = false, length = 16)
	@Enumerated(STRING)
	private Status status;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String payload;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String summary;

	@Column(nullable = false)
	private LocalDateTime expiredAt;

	public ImportBatch toDomain() {
		return ImportBatch.builder()
				.id(id)
				.version(version)
				.uploadedBy(uploadedBy)
				.sectionId(sectionId)
				.type(type)
				.status(status)
				.payload(JsonConverter.parse(payload, ImportBatchPayloadInvalidException::new))
				.summary(JsonConverter.parse(summary, ImportBatchPayloadInvalidException::new))
				.expiredAt(expiredAt)
				.createdAt(getCreatedAt())
				.updatedAt(getUpdatedAt())
				.deletedAt(getDeletedAt())
				.build();
	}

	public static ImportBatchJpaEntity toEntity(ImportBatch importBatch) {
		ImportBatchJpaEntity entity = ImportBatchJpaEntity.builder()
				.id(importBatch.getId())
				.version(importBatch.getVersion())
				.uploadedBy(importBatch.getUploadedBy())
				.sectionId(importBatch.getSectionId())
				.type(importBatch.getType())
				.status(importBatch.getStatus())
				.payload(importBatch.getPayload().toString())
				.summary(importBatch.getSummary().toString())
				.expiredAt(importBatch.getExpiredAt())
				.build();
		entity.createdAt = importBatch.getCreatedAt();
		entity.setDeletedAt(importBatch.getDeletedAt());
		return entity;
	}
}
