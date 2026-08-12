package kgu.developers.domain.evaluation.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TeamEvaluation {
    private static final int RATER_ID_MAX_LENGTH = 16;

    private final Long id;
    private final Long milestoneId;
    private final String raterId;
    private final Long rateeTeamId;
    private LocalDateTime submittedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private TeamEvaluation(
            Long id,
            Long milestoneId,
            String raterId,
            Long rateeTeamId,
            LocalDateTime submittedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validatePositiveId(id, "팀간 발표평가 id");
        validateRequiredPositive(milestoneId, "마일스톤 id");
        String normalizedRaterId = normalizeRaterId(raterId);
        validateRequiredPositive(rateeTeamId, "피평가 팀 id");

        this.id = id;
        this.milestoneId = milestoneId;
        this.raterId = normalizedRaterId;
        this.rateeTeamId = rateeTeamId;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static TeamEvaluation create(Long milestoneId, String raterId, Long rateeTeamId) {
        return new TeamEvaluation(null, milestoneId, raterId, rateeTeamId, null, null, null, null);
    }

    public static TeamEvaluation restore(
            Long id,
            Long milestoneId,
            String raterId,
            Long rateeTeamId,
            LocalDateTime submittedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateRequiredPositive(id, "팀간 발표평가 id");
        return new TeamEvaluation(id, milestoneId, raterId, rateeTeamId, submittedAt, createdAt, updatedAt, deletedAt);
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    public void submit(LocalDateTime submittedAt) {
        if (submittedAt == null) {
            throw new IllegalArgumentException("제출 시각은 비어 있을 수 없습니다.");
        }
        this.submittedAt = submittedAt;
    }

    private static String normalizeRaterId(String raterId) {
        if (raterId == null || raterId.isBlank()) {
            throw new IllegalArgumentException("평가자 학번은 비어 있을 수 없습니다.");
        }
        String normalizedRaterId = raterId.trim();
        if (normalizedRaterId.length() > RATER_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("평가자 학번은 16자를 초과할 수 없습니다.");
        }
        return normalizedRaterId;
    }

    private static void validatePositiveId(Long value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }

    private static void validateRequiredPositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }
}
