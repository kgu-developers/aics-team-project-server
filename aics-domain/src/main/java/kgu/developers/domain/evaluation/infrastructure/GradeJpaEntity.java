package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "grade",
        indexes = {
                @Index(name = "idx_grade_section_team", columnList = "section_id, team_id"),
                @Index(name = "idx_grade_user", columnList = "user_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class GradeJpaEntity extends BaseTimeEntity {
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "team_score", precision = 10, scale = 2)
    private BigDecimal teamScore;

    @Column(name = "peer_factor", precision = 10, scale = 4)
    private BigDecimal peerFactor;

    @Column(name = "final_score", precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "manual_adjustment", precision = 10, scale = 2)
    private BigDecimal manualAdjustment;

    @Column(name = "adjustment_reason", length = 2000)
    private String adjustmentReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode snapshot;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    public Grade toDomain() {
        return Grade.restore(id, sectionId, teamId, userId, teamScore, peerFactor, finalScore, manualAdjustment, adjustmentReason, snapshot == null ? null : snapshot.toString(), finalizedAt, getCreatedAt(), getUpdatedAt(), getDeletedAt());
    }

    public static GradeJpaEntity toEntity(Grade grade) {
        GradeJpaEntity entity = GradeJpaEntity.builder()
                .id(grade.getId())
                .sectionId(grade.getSectionId())
                .teamId(grade.getTeamId())
                .userId(grade.getUserId())
                .teamScore(grade.getTeamScore())
                .peerFactor(grade.getPeerFactor())
                .finalScore(grade.getFinalScore())
                .manualAdjustment(grade.getManualAdjustment())
                .adjustmentReason(grade.getAdjustmentReason())
                .snapshot(parseSnapshot(grade.getSnapshot()))
                .finalizedAt(grade.getFinalizedAt())
                .build();
        entity.createdAt = grade.getCreatedAt();
        entity.setDeletedAt(grade.getDeletedAt());
        return entity;
    }

    private static JsonNode parseSnapshot(String snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return SNAPSHOT_MAPPER.readTree(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("성적 스냅샷은 유효한 JSON이어야 합니다.", exception);
        }
    }
}
