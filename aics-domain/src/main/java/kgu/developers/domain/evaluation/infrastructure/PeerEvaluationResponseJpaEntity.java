package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.PeerEvaluationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "peer_evaluation_response",
        indexes = {
                @Index(name = "idx_peer_evaluation_response_form", columnList = "form_id"),
                @Index(name = "idx_peer_evaluation_response_target", columnList = "target_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationResponseJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "evaluator_id", nullable = false, length = 16)
    private String evaluatorId;

    @Column(name = "target_id", nullable = false, length = 16)
    private String targetId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "self_contribution", precision = 10, scale = 2)
    private BigDecimal selfContribution;

    @Column(name = "project_review_comment", length = 2000)
    private String projectReviewComment;

    public PeerEvaluationResponse toDomain() {
        return PeerEvaluationResponse.restore(id, formId, evaluatorId, targetId, submittedAt, selfContribution, projectReviewComment, getCreatedAt(), getUpdatedAt(), getDeletedAt());
    }

    public static PeerEvaluationResponseJpaEntity toEntity(PeerEvaluationResponse response) {
        PeerEvaluationResponseJpaEntity entity = PeerEvaluationResponseJpaEntity.builder()
                .id(response.getId())
                .formId(response.getFormId())
                .evaluatorId(response.getEvaluatorId())
                .targetId(response.getTargetId())
                .submittedAt(response.getSubmittedAt())
                .selfContribution(response.getSelfContribution())
                .projectReviewComment(response.getProjectReviewComment())
                .build();
        entity.createdAt = response.getCreatedAt();
        entity.setDeletedAt(response.getDeletedAt());
        return entity;
    }
}
