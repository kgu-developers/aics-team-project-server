package evaluation.domain;

import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamEvaluationDomainTest {

    @Test
    @DisplayName("평가 항목 생성은 제목 앞뒤 공백을 제거하고 전달받은 값으로 팀간 발표평가 항목을 생성한다")
    void createCriterion() {
        TeamEvaluationCriterion criterion = TeamEvaluationCriterion.create(1L, " 발표 완성도 ", 10, 2);

        assertThat(criterion.getId()).isNull();
        assertThat(criterion.getSectionId()).isEqualTo(1L);
        assertThat(criterion.getTitle()).isEqualTo("발표 완성도");
        assertThat(criterion.getMaxScore()).isEqualTo(10);
        assertThat(criterion.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("평가 항목 생성은 필수값이 잘못되면 예외를 던진다")
    void createCriterionValidation() {
        assertThatThrownBy(() -> TeamEvaluationCriterion.create(null, "발표 완성도", 10, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분반 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationCriterion.create(1L, " ", 10, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가 항목 제목은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> TeamEvaluationCriterion.create(1L, "발표 완성도", 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 점수는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationCriterion.create(1L, "발표 완성도", 10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("표시 순서는 0 이상이어야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationCriterion.create(1L, "가".repeat(101), 10, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가 항목 제목은 100자를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("팀간 발표평가 생성은 평가자 학번 앞뒤 공백을 제거하고 제출되지 않은 초안을 생성한다")
    void createEvaluation() {
        String maximumLengthRaterId = "1".repeat(20);
        TeamEvaluation evaluation = TeamEvaluation.create(2L, " " + maximumLengthRaterId + " ", 3L);

        assertThat(evaluation.getId()).isNull();
        assertThat(evaluation.getMilestoneId()).isEqualTo(2L);
        assertThat(evaluation.getRaterId()).isEqualTo(maximumLengthRaterId);
        assertThat(evaluation.getRateeTeamId()).isEqualTo(3L);
        assertThat(evaluation.getSubmittedAt()).isNull();
        assertThat(evaluation.isSubmitted()).isFalse();
    }

    @Test
    @DisplayName("팀간 발표평가는 제출 시각을 기록하면 제출 상태가 된다")
    void submitEvaluation() {
        TeamEvaluation evaluation = TeamEvaluation.create(2L, "20260001", 3L);
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 12, 10, 30);

        evaluation.submit(submittedAt);

        assertThat(evaluation.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(evaluation.isSubmitted()).isTrue();
    }

    @Test
    @DisplayName("팀간 발표평가 생성은 필수 식별자가 잘못되면 예외를 던진다")
    void createEvaluationValidation() {
        assertThatThrownBy(() -> TeamEvaluation.create(0L, "20260001", 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("마일스톤 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluation.create(2L, " ", 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가자 학번은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> TeamEvaluation.create(2L, "20260001", -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("피평가 팀 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluation.create(2L, "20260001", 3L).submit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제출 시각은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> TeamEvaluation.create(2L, "1".repeat(21), 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가자 학번은 20자를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("평가 점수 생성은 0 이상이면서 항목 최대 점수 이하여야 한다")
    void createScore() {
        TeamEvaluationScore score = TeamEvaluationScore.create(1L, 2L, 15, 20);

        assertThat(score.getId()).isNull();
        assertThat(score.getTeamEvaluationId()).isEqualTo(1L);
        assertThat(score.getCriterionId()).isEqualTo(2L);
        assertThat(score.getScore()).isEqualTo(15);
    }

    @Test
    @DisplayName("평가 점수 생성은 필수값이 잘못되면 예외를 던진다")
    void createScoreValidation() {
        assertThatThrownBy(() -> TeamEvaluationScore.create(null, 2L, 5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀간 발표평가 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationScore.create(1L, 0L, 5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가 항목 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationScore.create(1L, 2L, -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("점수는 0 이상이어야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationScore.create(1L, 2L, 11, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("점수는 최대 점수를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("평가 점수는 초안 덮어쓰기와 제출 후 재수정을 위해 점수를 변경할 수 있다")
    void changeScore() {
        TeamEvaluationScore score = TeamEvaluationScore.create(1L, 2L, 5, 10);

        score.changeScore(9, 10);

        assertThat(score.getScore()).isEqualTo(9);
        assertThatThrownBy(() -> score.changeScore(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("점수는 0 이상이어야 합니다.");
        assertThatThrownBy(() -> score.changeScore(11, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("점수는 최대 점수를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("복원은 모든 엔티티의 id가 없으면 예외를 던진다")
    void restoreRequiresId() {
        assertThatThrownBy(() -> TeamEvaluationCriterion.restore(null, 1L, "발표 완성도", 10, 1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가 항목 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluation.restore(null, 1L, "20260001", 2L, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀간 발표평가 id는 양수여야 합니다.");
        assertThatThrownBy(() -> TeamEvaluationScore.restore(null, 1L, 2L, 5, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가 점수 id는 양수여야 합니다.");
    }
}
