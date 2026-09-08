package kgu.developers.domain.evaluation.domain;

import java.time.LocalDateTime;
import kgu.developers.domain.evaluation.exception.PeerEvaluationAlreadySubmittedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationSubmission {
    private Long id;
    private Long formId;
    private String evaluatorId;
    private String selfContribution;
    private String projectReviewComment;
    private String reflectionComment;
    private PeerEvaluationSubmissionStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static PeerEvaluationSubmission create(Long formId, String evaluatorId) {
        return PeerEvaluationSubmission.builder()
            .formId(formId)
            .evaluatorId(evaluatorId)
            .status(PeerEvaluationSubmissionStatus.DRAFT)
            .build();
    }

    public void update(
        String selfContribution,
        String projectReviewComment,
        String reflectionComment,
        boolean submit,
        LocalDateTime now
    ) {
        if (status == PeerEvaluationSubmissionStatus.SUBMITTED) {
            throw new PeerEvaluationAlreadySubmittedException();
        }
        this.selfContribution = normalize(selfContribution, 2000);
        this.projectReviewComment = normalize(projectReviewComment, 2000);
        this.reflectionComment = normalize(reflectionComment, 2000);
        this.status = submit ? PeerEvaluationSubmissionStatus.SUBMITTED : PeerEvaluationSubmissionStatus.DRAFT;
        this.submittedAt = submit ? now : null;
    }

    private static String normalize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("상호평가 서술 답변은 2000자를 넘을 수 없습니다.");
        }
        return normalized;
    }
}
