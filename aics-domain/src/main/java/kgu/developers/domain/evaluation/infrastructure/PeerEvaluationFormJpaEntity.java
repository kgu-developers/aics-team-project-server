package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "peer_evaluation_form",
        indexes = {
                @Index(name = "idx_peer_evaluation_form_section_milestone", columnList = "section_id, milestone_id"),
                @Index(name = "idx_peer_evaluation_form_period", columnList = "opens_at, closes_at")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationFormJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "opens_at", nullable = false)
    private LocalDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    public PeerEvaluationForm toDomain() {
        return PeerEvaluationForm.restore(id, sectionId, milestoneId, anonymous, opensAt, closesAt, getCreatedAt(), getUpdatedAt(), getDeletedAt());
    }

    public static PeerEvaluationFormJpaEntity toEntity(PeerEvaluationForm form) {
        PeerEvaluationFormJpaEntity entity = PeerEvaluationFormJpaEntity.builder()
                .id(form.getId())
                .sectionId(form.getSectionId())
                .milestoneId(form.getMilestoneId())
                .anonymous(form.isAnonymous())
                .opensAt(form.getOpensAt())
                .closesAt(form.getClosesAt())
                .build();
        entity.createdAt = form.getCreatedAt();
        entity.setDeletedAt(form.getDeletedAt());
        return entity;
    }
}
