package evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.evaluation.domain.Grade;
import kgu.developers.domain.evaluation.domain.PeerEvaluationAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestion;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestionType;
import kgu.developers.domain.evaluation.domain.PeerEvaluationResponse;

class PeerEvaluationDomainTest {
    private final LocalDateTime opensAt = LocalDateTime.of(2026, 8, 1, 9, 0);
    private final LocalDateTime closesAt = LocalDateTime.of(2026, 8, 7, 18, 0);

    @Test
    @DisplayName("상호평가 양식은 분반과 마일스톤 식별자 및 기간으로 생성된다")
    void createForm() {
        PeerEvaluationForm form = PeerEvaluationForm.create(1L, 2L, true, opensAt, closesAt);

        assertThat(form.getSectionId()).isEqualTo(1L);
        assertThat(form.getMilestoneId()).isEqualTo(2L);
        assertThat(form.isAnonymous()).isTrue();
        assertThat(form.getOpensAt()).isEqualTo(opensAt);
        assertThat(form.getClosesAt()).isEqualTo(closesAt);
    }

    @Test
    @DisplayName("상호평가 양식은 시작 시각이 종료 시각보다 빠르지 않으면 생성할 수 없다")
    void rejectInvalidFormPeriod() {
        assertThatThrownBy(() -> PeerEvaluationForm.create(1L, 2L, false, closesAt, opensAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상호평가 시작 시각은 종료 시각보다 앞서야 합니다.");
    }

    @Test
    @DisplayName("점수형 질문은 양수 최대 점수를 가져야 한다")
    void scaleQuestionRequiresPositiveMaxScore() {
        PeerEvaluationQuestion question = PeerEvaluationQuestion.createScale(1L, "협업 태도를 평가하세요.", new BigDecimal("5.00"), 0);

        assertThat(question.getType()).isEqualTo(PeerEvaluationQuestionType.SCALE);
        assertThat(question.getMaxScore()).isEqualByComparingTo("5.00");

        assertThatThrownBy(() -> PeerEvaluationQuestion.createScale(1L, "협업 태도를 평가하세요.", BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("점수형 질문의 최대 점수는 양수여야 합니다.");
    }

    @Test
    @DisplayName("질문 내용은 앞뒤 공백을 제거하고 500자를 넘을 수 없다")
    void questionTextIsTrimmedAndLimited() {
        PeerEvaluationQuestion question = PeerEvaluationQuestion.createText(1L, "  개선 의견을 작성하세요.  ", 1);

        assertThat(question.getText()).isEqualTo("개선 의견을 작성하세요.");

        assertThatThrownBy(() -> PeerEvaluationQuestion.createText(1L, "가".repeat(501), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("질문 내용은 500자를 넘을 수 없습니다.");
    }

    @Test
    @DisplayName("서술형 질문은 최대 점수를 갖지 않는다")
    void textQuestionHasNoMaxScore() {
        PeerEvaluationQuestion question = PeerEvaluationQuestion.createText(1L, "개선 의견을 작성하세요.", 1);

        assertThat(question.getType()).isEqualTo(PeerEvaluationQuestionType.TEXT);
        assertThat(question.getMaxScore()).isNull();

        assertThatThrownBy(() -> PeerEvaluationQuestion.restore(1L, 1L, "개선 의견을 작성하세요.", PeerEvaluationQuestionType.TEXT, BigDecimal.ONE, 1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("서술형 질문은 최대 점수를 가질 수 없습니다.");
    }

    @Test
    @DisplayName("상호평가 응답은 제출 전 상태로 생성되고 제출 시각을 기록할 수 있다")
    void createAndSubmitResponse() {
        PeerEvaluationResponse response = PeerEvaluationResponse.create(1L, " 20260001 ", " 20260002 ", new BigDecimal("35.50"), " 프로젝트 진행이 원활했습니다. ");

        assertThat(response.getSubmittedAt()).isNull();
        assertThat(response.getEvaluatorId()).isEqualTo("20260001");
        assertThat(response.getTargetId()).isEqualTo("20260002");
        assertThat(response.getProjectReviewComment()).isEqualTo("프로젝트 진행이 원활했습니다.");

        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        response.submit(submittedAt);

        assertThat(response.getSubmittedAt()).isEqualTo(submittedAt);
    }

    @Test
    @DisplayName("상호평가 응답의 본인 기여도는 0 이상 100 이하이어야 한다")
    void rejectOutOfRangeSelfContribution() {
        assertThatThrownBy(() -> PeerEvaluationResponse.create(1L, "20260001", "20260002", new BigDecimal("-0.01"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 기여도는 0 이상 100 이하이어야 합니다.");
        assertThatThrownBy(() -> PeerEvaluationResponse.create(1L, "20260001", "20260002", new BigDecimal("100.01"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 기여도는 0 이상 100 이하이어야 합니다.");
    }

    @Test
    @DisplayName("상호평가 응답의 본인 기여도는 소수 둘째 자리까지만 허용한다")
    void rejectSelfContributionWithExcessScale() {
        assertThatThrownBy(() -> PeerEvaluationResponse.create(1L, "20260001", "20260002", new BigDecimal("99.999"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 기여도는 소수 둘째 자리까지만 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("상호평가 응답의 학번과 회고 의견은 최대 길이를 넘을 수 없다")
    void responseTextLengthIsLimited() {
        assertThatThrownBy(() -> PeerEvaluationResponse.create(1L, "1".repeat(17), "20260002", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가자 학번은 16자를 넘을 수 없습니다.");
        assertThatThrownBy(() -> PeerEvaluationResponse.create(1L, "20260001", "2".repeat(17), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평가 대상자 학번은 16자를 넘을 수 없습니다.");
        assertThatThrownBy(() -> PeerEvaluationResponse.create(1L, "20260001", "20260002", null, "가".repeat(2001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("프로젝트 회고 의견은 2000자를 넘을 수 없습니다.");
    }

    @Test
    @DisplayName("답변은 점수 또는 서술 답변 중 정확히 하나만 가진다")
    void answerHasExactlyOneValue() {
        PeerEvaluationAnswer scoreAnswer = PeerEvaluationAnswer.createScore(
                1L,
                2L,
                new BigDecimal("4.50"),
                new BigDecimal("5.00")
        );
        PeerEvaluationAnswer textAnswer = PeerEvaluationAnswer.createText(1L, 3L, " 팀원과 소통이 좋았습니다. ");

        assertThat(scoreAnswer.getScore()).isEqualByComparingTo("4.50");
        assertThat(scoreAnswer.getTextAnswer()).isNull();
        assertThat(textAnswer.getScore()).isNull();
        assertThat(textAnswer.getTextAnswer()).isEqualTo("팀원과 소통이 좋았습니다.");

        assertThatThrownBy(() -> PeerEvaluationAnswer.restore(1L, 1L, 2L, new BigDecimal("4.00"), "중복 답변", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("답변은 점수 또는 서술 답변 중 정확히 하나만 가져야 합니다.");
        assertThatThrownBy(() -> PeerEvaluationAnswer.restore(1L, 1L, 2L, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("답변은 점수 또는 서술 답변 중 정확히 하나만 가져야 합니다.");
        assertThatThrownBy(() -> PeerEvaluationAnswer.createScore(
                1L,
                2L,
                new BigDecimal("5.01"),
                new BigDecimal("5.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("답변 점수는 질문의 최대 점수를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("서술 답변은 최대 길이를 넘을 수 없다")
    void answerTextLengthIsLimited() {
        assertThatThrownBy(() -> PeerEvaluationAnswer.createText(1L, 2L, "가".repeat(2001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("서술 답변은 2000자를 넘을 수 없습니다.");
    }

    @Test
    @DisplayName("성적은 계산식 없이 전달된 점수와 JSON 스냅샷 원문을 보존한다")
    void createGrade() {
        String snapshot = "{\"teamScore\":90,\"peerFactor\":1.05}";

        Grade grade = Grade.create(
                1L,
                10L,
                " 20260001 ",
                new BigDecimal("90.00"),
                new BigDecimal("1.0500"),
                new BigDecimal("94.50"),
                new BigDecimal("-1.00"),
                snapshot
        );

        assertThat(grade.getUserId()).isEqualTo("20260001");
        assertThat(grade.getTeamScore()).isEqualByComparingTo("90.00");
        assertThat(grade.getPeerFactor()).isEqualByComparingTo("1.0500");
        assertThat(grade.getFinalScore()).isEqualByComparingTo("94.50");
        assertThat(grade.getManualAdjustment()).isEqualByComparingTo("-1.00");
        assertThat(grade.getSnapshot()).isEqualTo(snapshot);
    }

    @Test
    @DisplayName("성적의 점수 계열 값은 0 이상이어야 하고 수동 조정 점수는 음수를 허용한다")
    void gradeScoresAreLimited() {
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", new BigDecimal("-0.01"), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀 점수는 0 이상이어야 합니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, new BigDecimal("-0.01"), BigDecimal.TEN, BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동료평가 계수는 0 이상이어야 합니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("-0.01"), BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최종 점수는 0 이상이어야 합니다.");

        Grade grade = Grade.create(1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("-5.00"), "{}");

        assertThat(grade.getManualAdjustment()).isEqualByComparingTo("-5.00");
    }

    @Test
    @DisplayName("성적의 점수 계열 값은 모두 필수이다")
    void gradeScoresAreRequired() {
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", null, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀 점수는 필수입니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, null, BigDecimal.TEN, BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동료평가 계수는 필수입니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, null, BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최종 점수는 필수입니다.");
    }

    @Test
    @DisplayName("성적의 학번은 최대 16자이고 스냅샷은 비어 있을 수 없다")
    void gradeUserAndSnapshotAreValidated() {
        assertThatThrownBy(() -> Grade.create(1L, 10L, "1".repeat(17), BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학번은 16자를 넘을 수 없습니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("성적 스냅샷은 필수입니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("성적 스냅샷은 유효한 JSON이어야 합니다.");
        assertThatThrownBy(() -> Grade.create(1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "{} {}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("성적 스냅샷은 유효한 JSON이어야 합니다.");
    }

    @Test
    @DisplayName("복원은 모든 엔티티의 식별자가 양수일 때만 허용된다")
    void restoreRequiresPositiveId() {
        assertThatThrownBy(() -> PeerEvaluationForm.restore(null, 1L, 2L, false, opensAt, closesAt, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상호평가 양식 식별자는 양수여야 합니다.");
        assertThatThrownBy(() -> PeerEvaluationQuestion.restore(0L, 1L, "질문", PeerEvaluationQuestionType.TEXT, null, 1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상호평가 질문 식별자는 양수여야 합니다.");
        assertThatThrownBy(() -> PeerEvaluationResponse.restore(null, 1L, "20260001", "20260002", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상호평가 응답 식별자는 양수여야 합니다.");
        assertThatThrownBy(() -> PeerEvaluationAnswer.restore(null, 1L, 2L, BigDecimal.ONE, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상호평가 답변 식별자는 양수여야 합니다.");
        assertThatThrownBy(() -> Grade.restore(null, 1L, 10L, "20260001", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, "{}", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("성적 식별자는 양수여야 합니다.");
    }
}
