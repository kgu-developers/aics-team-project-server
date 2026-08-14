package kgu.developers.domain.evaluation.domain;

import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requirePositiveId;
import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requireTrimmedText;
import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.trimNullableText;
import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = PRIVATE)
public class PeerEvaluationResponse {
    private Long id;
    private Long formId;
    private String evaluatorId;
    private String targetId;
    private LocalDateTime submittedAt;
    private BigDecimal selfContribution;
    private String projectReviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private PeerEvaluationResponse(
            Long id,
            Long formId,
            String evaluatorId,
            String targetId,
            LocalDateTime submittedAt,
            BigDecimal selfContribution,
            String projectReviewComment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(formId, "상호평가 양식 식별자는 양수여야 합니다.");
        String normalizedEvaluatorId = requireTrimmedText(evaluatorId, 20, "평가자 학번은 필수입니다.", "평가자 학번은 20자를 넘을 수 없습니다.");
        String normalizedTargetId = requireTrimmedText(targetId, 20, "평가 대상자 학번은 필수입니다.", "평가 대상자 학번은 20자를 넘을 수 없습니다.");
        String normalizedComment = trimNullableText(projectReviewComment, 2000, "프로젝트 회고 의견은 2000자를 넘을 수 없습니다.");
        if (selfContribution != null && (selfContribution.signum() < 0 || selfContribution.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("본인 기여도는 0 이상 100 이하이어야 합니다.");
        }
        if (selfContribution != null && selfContribution.scale() > 2) {
            throw new IllegalArgumentException("본인 기여도는 소수 둘째 자리까지만 입력할 수 있습니다.");
        }

        this.id = id;
        this.formId = formId;
        this.evaluatorId = normalizedEvaluatorId;
        this.targetId = normalizedTargetId;
        this.submittedAt = submittedAt;
        this.selfContribution = selfContribution;
        this.projectReviewComment = normalizedComment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static PeerEvaluationResponse create(
            Long formId,
            String evaluatorId,
            String targetId,
            BigDecimal selfContribution,
            String projectReviewComment
    ) {
        return new PeerEvaluationResponse(null, formId, evaluatorId, targetId, null, selfContribution, projectReviewComment, null, null, null);
    }

    public static PeerEvaluationResponse restore(
            Long id,
            Long formId,
            String evaluatorId,
            String targetId,
            LocalDateTime submittedAt,
            BigDecimal selfContribution,
            String projectReviewComment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(id, "상호평가 응답 식별자는 양수여야 합니다.");
        return new PeerEvaluationResponse(id, formId, evaluatorId, targetId, submittedAt, selfContribution, projectReviewComment, createdAt, updatedAt, deletedAt);
    }

    public void submit(LocalDateTime submittedAt) {
        validateRequired(submittedAt, "제출 시각은 필수입니다.");
        this.submittedAt = submittedAt;
    }

    private static void validateRequired(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
