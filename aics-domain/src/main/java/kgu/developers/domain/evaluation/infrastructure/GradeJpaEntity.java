package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;

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
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false, length = 16)
    private String userId;

    @Column(name = "team_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal teamScore;

    @Column(name = "peer_factor", nullable = false, precision = 10, scale = 4)
    private BigDecimal peerFactor;

    @Column(name = "final_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "manual_adjustment", nullable = false, precision = 10, scale = 2)
    private BigDecimal manualAdjustment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    public Grade toDomain() {
        return Grade.restore(id, sectionId, teamId, userId, teamScore, peerFactor, finalScore, manualAdjustment, snapshot, getCreatedAt(), getUpdatedAt(), getDeletedAt());
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
                .snapshot(grade.getSnapshot())
                .build();
        entity.createdAt = grade.getCreatedAt();
        entity.setDeletedAt(grade.getDeletedAt());
        return entity;
    }
}
