package evaluation.infrastructure;

import jakarta.persistence.Column;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationCriterionJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationScoreJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TeamEvaluationJpaEntityTest {

    @Test
    @DisplayName("평가자 학번 JPA 컬럼은 사용자 학번과 동일하게 20자를 허용한다")
    void raterIdColumnLength() throws NoSuchFieldException {
        Column column = TeamEvaluationJpaEntity.class
                .getDeclaredField("raterId")
                .getAnnotation(Column.class);

        assertThat(column.length()).isEqualTo(20);
    }

    @Test
    @DisplayName("평가 항목 JPA entity는 domain과 왕복 매핑된다")
    void criterionRoundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 12, 9, 0);
        TeamEvaluationCriterion criterion = TeamEvaluationCriterion.restore(
                1L,
                2L,
                "자료 적절성",
                10,
                3,
                createdAt,
                null,
                null
        );

        TeamEvaluationCriterion mapped = TeamEvaluationCriterionJpaEntity.toEntity(criterion).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getSectionId()).isEqualTo(2L);
        assertThat(mapped.getTitle()).isEqualTo("자료 적절성");
        assertThat(mapped.getMaxScore()).isEqualTo(10);
        assertThat(mapped.getDisplayOrder()).isEqualTo(3);
        assertThat(mapped.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("팀간 발표평가 JPA entity는 제출 시각을 포함해 domain과 왕복 매핑된다")
    void evaluationRoundTrip() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 12, 10, 0);
        TeamEvaluation evaluation = TeamEvaluation.restore(
                1L,
                2L,
                " 20260001 ",
                3L,
                submittedAt,
                null,
                null,
                null
        );

        TeamEvaluation mapped = TeamEvaluationJpaEntity.toEntity(evaluation).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getMilestoneId()).isEqualTo(2L);
        assertThat(mapped.getRaterId()).isEqualTo("20260001");
        assertThat(mapped.getRateeTeamId()).isEqualTo(3L);
        assertThat(mapped.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(mapped.isSubmitted()).isTrue();
    }

    @Test
    @DisplayName("평가 점수 JPA entity는 스칼라 식별자를 유지하며 domain과 왕복 매핑된다")
    void scoreRoundTrip() {
        TeamEvaluationScore score = TeamEvaluationScore.restore(
                1L,
                2L,
                3L,
                8,
                null,
                null,
                null
        );

        TeamEvaluationScore mapped = TeamEvaluationScoreJpaEntity.toEntity(score).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getTeamEvaluationId()).isEqualTo(2L);
        assertThat(mapped.getCriterionId()).isEqualTo(3L);
        assertThat(mapped.getScore()).isEqualTo(8);
    }
}
