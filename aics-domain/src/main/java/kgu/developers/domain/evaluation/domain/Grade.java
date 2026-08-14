package kgu.developers.domain.evaluation.domain;

import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requireNonNegative;
import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requirePositiveId;
import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requireTrimmedText;
import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.trimNullableText;
import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = PRIVATE)
public class Grade {
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private Long id;
    private Long sectionId;
    private Long teamId;
    private String userId;
    private BigDecimal teamScore;
    private BigDecimal peerFactor;
    private BigDecimal finalScore;
    private BigDecimal manualAdjustment;
    private String adjustmentReason;
    private String snapshot;
    private LocalDateTime finalizedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Grade(
            Long id,
            Long sectionId,
            Long teamId,
            String userId,
            BigDecimal teamScore,
            BigDecimal peerFactor,
            BigDecimal finalScore,
            BigDecimal manualAdjustment,
            String adjustmentReason,
            String snapshot,
            LocalDateTime finalizedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(sectionId, "분반 식별자는 양수여야 합니다.");
        requirePositiveId(teamId, "팀 식별자는 양수여야 합니다.");
        String normalizedUserId = requireTrimmedText(userId, 20, "학번은 필수입니다.", "학번은 20자를 넘을 수 없습니다.");
        validateNonNegativeIfPresent(teamScore, "팀 점수는 0 이상이어야 합니다.");
        validateNonNegativeIfPresent(peerFactor, "동료평가 계수는 0 이상이어야 합니다.");
        validateNonNegativeIfPresent(finalScore, "최종 점수는 0 이상이어야 합니다.");
        String normalizedAdjustmentReason = trimNullableText(adjustmentReason, 2000, "수동 조정 사유는 2000자를 넘을 수 없습니다.");
        validateSnapshot(snapshot);
        validateFinalizedState(finalizedAt, teamScore, peerFactor, finalScore, snapshot);

        this.id = id;
        this.sectionId = sectionId;
        this.teamId = teamId;
        this.userId = normalizedUserId;
        this.teamScore = teamScore;
        this.peerFactor = peerFactor;
        this.finalScore = finalScore;
        this.manualAdjustment = manualAdjustment;
        this.adjustmentReason = normalizedAdjustmentReason;
        this.snapshot = snapshot;
        this.finalizedAt = finalizedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Grade create(
            Long sectionId,
            Long teamId,
            String userId,
            BigDecimal teamScore,
            BigDecimal peerFactor,
            BigDecimal finalScore,
            BigDecimal manualAdjustment,
            String snapshot
    ) {
        return create(sectionId, teamId, userId, teamScore, peerFactor, finalScore, manualAdjustment, null, snapshot, null);
    }

    public static Grade create(
            Long sectionId,
            Long teamId,
            String userId,
            BigDecimal teamScore,
            BigDecimal peerFactor,
            BigDecimal finalScore,
            BigDecimal manualAdjustment,
            String adjustmentReason,
            String snapshot,
            LocalDateTime finalizedAt
    ) {
        return new Grade(null, sectionId, teamId, userId, teamScore, peerFactor, finalScore, manualAdjustment, adjustmentReason, snapshot, finalizedAt, null, null, null);
    }

    public static Grade restore(
            Long id,
            Long sectionId,
            Long teamId,
            String userId,
            BigDecimal teamScore,
            BigDecimal peerFactor,
            BigDecimal finalScore,
            BigDecimal manualAdjustment,
            String snapshot,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return restore(id, sectionId, teamId, userId, teamScore, peerFactor, finalScore, manualAdjustment, null, snapshot, null, createdAt, updatedAt, deletedAt);
    }

    public static Grade restore(
            Long id,
            Long sectionId,
            Long teamId,
            String userId,
            BigDecimal teamScore,
            BigDecimal peerFactor,
            BigDecimal finalScore,
            BigDecimal manualAdjustment,
            String adjustmentReason,
            String snapshot,
            LocalDateTime finalizedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(id, "성적 식별자는 양수여야 합니다.");
        return new Grade(id, sectionId, teamId, userId, teamScore, peerFactor, finalScore, manualAdjustment, adjustmentReason, snapshot, finalizedAt, createdAt, updatedAt, deletedAt);
    }

    private static void validateNonNegativeIfPresent(BigDecimal value, String message) {
        if (value != null) {
            requireNonNegative(value, message);
        }
    }

    private static void validateSnapshot(String snapshot) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.isBlank()) {
            throw new IllegalArgumentException("성적 스냅샷은 필수입니다.");
        }
        try {
            SNAPSHOT_MAPPER.readTree(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("성적 스냅샷은 유효한 JSON이어야 합니다.", exception);
        }
    }

    private static void validateFinalizedState(
            LocalDateTime finalizedAt,
            BigDecimal teamScore,
            BigDecimal peerFactor,
            BigDecimal finalScore,
            String snapshot
    ) {
        if (finalizedAt != null
                && (teamScore == null || peerFactor == null || finalScore == null || snapshot == null)) {
            throw new IllegalArgumentException("확정된 성적은 팀 점수, 동료평가 계수, 최종 점수와 스냅샷이 모두 필요합니다.");
        }
    }
}
