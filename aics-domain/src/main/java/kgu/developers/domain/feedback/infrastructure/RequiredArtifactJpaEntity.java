package kgu.developers.domain.feedback.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "required_artifact",
        indexes = {
                @Index(name = "idx_required_artifact_milestone", columnList = "milestone_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class RequiredArtifactJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private RequiredArtifactType type;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "allowed_extensions", length = 255)
    private String allowedExtensions;

    @Column(name = "max_file_size_mb")
    private Integer maxFileSizeMb;

    public RequiredArtifact toDomain() {
        return RequiredArtifact.restore(
                id,
                milestoneId,
                type,
                label,
                required,
                allowedExtensions,
                maxFileSizeMb,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }

    public static RequiredArtifactJpaEntity toEntity(RequiredArtifact requiredArtifact) {
        RequiredArtifactJpaEntity entity = RequiredArtifactJpaEntity.builder()
                .id(requiredArtifact.getId())
                .milestoneId(requiredArtifact.getMilestoneId())
                .type(requiredArtifact.getType())
                .label(requiredArtifact.getLabel())
                .required(requiredArtifact.isRequired())
                .allowedExtensions(requiredArtifact.getAllowedExtensions())
                .maxFileSizeMb(requiredArtifact.getMaxFileSizeMb())
                .build();
        entity.createdAt = requiredArtifact.getCreatedAt();
        entity.updatedAt = requiredArtifact.getUpdatedAt();
        entity.setDeletedAt(requiredArtifact.getDeletedAt());
        return entity;
    }
}
