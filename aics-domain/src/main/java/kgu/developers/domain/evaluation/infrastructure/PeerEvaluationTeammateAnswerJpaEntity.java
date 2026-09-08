package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "peer_evaluation_teammate_answer",
    indexes = @Index(name = "idx_peer_evaluation_teammate_submission", columnList = "submission_id"),
    uniqueConstraints = @UniqueConstraint(
        name = "uk_peer_evaluation_teammate_submission_target",
        columnNames = {"submission_id", "target_user_id"}
    )
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationTeammateAnswerJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(name = "submission_id", nullable = false)
    private Long submissionId;
    @Column(name = "target_user_id", nullable = false, length = 20)
    private String targetUserId;
    @Column(name = "contribution_percent")
    private Integer contributionPercent;
    @Column(name = "contribution_detail", nullable = false, length = 2000)
    private String contributionDetail;
    @Column(name = "teammate_assessment", nullable = false, length = 2000)
    private String teammateAssessment;

    public PeerEvaluationTeammateAnswer toDomain() {
        return PeerEvaluationTeammateAnswer.builder()
            .id(id).submissionId(submissionId).targetUserId(targetUserId)
            .contributionPercent(contributionPercent).contributionDetail(contributionDetail)
            .teammateAssessment(teammateAssessment).build();
    }

    public static PeerEvaluationTeammateAnswerJpaEntity from(PeerEvaluationTeammateAnswer answer) {
        return PeerEvaluationTeammateAnswerJpaEntity.builder()
            .id(answer.getId()).submissionId(answer.getSubmissionId()).targetUserId(answer.getTargetUserId())
            .contributionPercent(answer.getContributionPercent()).contributionDetail(answer.getContributionDetail())
            .teammateAssessment(answer.getTeammateAssessment()).build();
    }
}
