package kgu.developers.domain.evaluation.domain;

import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requirePositiveId;
import static lombok.AccessLevel.PRIVATE;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = PRIVATE)
public class PeerEvaluationForm {
    private Long id;
    private Long sectionId;
    private Long milestoneId;
    private boolean anonymous;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private PeerEvaluationForm(
            Long id,
            Long sectionId,
            Long milestoneId,
            boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(sectionId, "분반 식별자는 양수여야 합니다.");
        requirePositiveId(milestoneId, "마일스톤 식별자는 양수여야 합니다.");
        validateRequired(opensAt, "상호평가 시작 시각은 필수입니다.");
        validateRequired(closesAt, "상호평가 종료 시각은 필수입니다.");
        if (!opensAt.isBefore(closesAt)) {
            throw new IllegalArgumentException("상호평가 시작 시각은 종료 시각보다 앞서야 합니다.");
        }

        this.id = id;
        this.sectionId = sectionId;
        this.milestoneId = milestoneId;
        this.anonymous = anonymous;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static PeerEvaluationForm create(
            Long sectionId,
            Long milestoneId,
            boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt
    ) {
        return new PeerEvaluationForm(null, sectionId, milestoneId, anonymous, opensAt, closesAt, null, null, null);
    }

    public static PeerEvaluationForm restore(
            Long id,
            Long sectionId,
            Long milestoneId,
            boolean anonymous,
            LocalDateTime opensAt,
            LocalDateTime closesAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(id, "상호평가 양식 식별자는 양수여야 합니다.");
        return new PeerEvaluationForm(id, sectionId, milestoneId, anonymous, opensAt, closesAt, createdAt, updatedAt, deletedAt);
    }

    private static void validateRequired(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
