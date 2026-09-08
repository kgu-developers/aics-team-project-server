package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "peer_evaluation_submission",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_peer_evaluation_submission_form_evaluator",
        columnNames = {"form_id", "evaluator_id"}
    )
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationSubmissionJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(name = "form_id", nullable = false)
    private Long formId;
    @Column(name = "evaluator_id", nullable = false, length = 20)
    private String evaluatorId;
    @Column(name = "self_contribution", nullable = false, length = 2000)
    private String selfContribution;
    @Column(name = "project_review_comment", nullable = false, length = 2000)
    private String projectReviewComment;
    @Column(name = "reflection_comment", nullable = false, length = 2000)
    private String reflectionComment;
    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private PeerEvaluationSubmissionStatus status;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public PeerEvaluationSubmission toDomain() {
        return PeerEvaluationSubmission.builder()
            .id(id).formId(formId).evaluatorId(evaluatorId)
            .selfContribution(selfContribution).projectReviewComment(projectReviewComment)
            .reflectionComment(reflectionComment).status(status).submittedAt(submittedAt)
            .createdAt(getCreatedAt()).updatedAt(getUpdatedAt()).deletedAt(getDeletedAt())
            .build();
    }

    public static PeerEvaluationSubmissionJpaEntity from(PeerEvaluationSubmission submission) {
        PeerEvaluationSubmissionJpaEntity entity = PeerEvaluationSubmissionJpaEntity.builder()
            .id(submission.getId()).formId(submission.getFormId()).evaluatorId(submission.getEvaluatorId())
            .selfContribution(submission.getSelfContribution()).projectReviewComment(submission.getProjectReviewComment())
            .reflectionComment(submission.getReflectionComment()).status(submission.getStatus())
            .submittedAt(submission.getSubmittedAt()).build();
        entity.createdAt = submission.getCreatedAt();
        entity.setDeletedAt(submission.getDeletedAt());
        return entity;
    }
}
