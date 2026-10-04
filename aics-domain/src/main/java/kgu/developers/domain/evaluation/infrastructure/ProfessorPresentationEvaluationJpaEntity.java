package kgu.developers.domain.evaluation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "professor_presentation_evaluation")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProfessorPresentationEvaluationJpaEntity extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;
    @Column(name = "team_id", nullable = false)
    private Long teamId;
    @Column(name = "professor_id", length = 20)
    private String professorId;
    @Column(columnDefinition = "text")
    private String memo;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public ProfessorPresentationEvaluation toDomain() {
        return ProfessorPresentationEvaluation.restore(id, milestoneId, teamId, professorId,
                memo, submittedAt, getCreatedAt(), getDeletedAt());
    }

    public static ProfessorPresentationEvaluationJpaEntity fromDomain(ProfessorPresentationEvaluation evaluation) {
        ProfessorPresentationEvaluationJpaEntity entity = ProfessorPresentationEvaluationJpaEntity.builder()
                .id(evaluation.getId())
                .milestoneId(evaluation.getMilestoneId())
                .teamId(evaluation.getTeamId())
                .professorId(evaluation.getProfessorId())
                .memo(evaluation.getMemo())
                .submittedAt(evaluation.getSubmittedAt())
                .build();
        entity.createdAt = evaluation.getCreatedAt();
        entity.setDeletedAt(evaluation.getDeletedAt());
        return entity;
    }
}
