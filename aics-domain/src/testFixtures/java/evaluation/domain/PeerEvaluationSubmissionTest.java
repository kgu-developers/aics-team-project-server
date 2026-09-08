package evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.exception.PeerEvaluationAlreadySubmittedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PeerEvaluationSubmissionTest {

    @Test
    @DisplayName("상호평가 최종 제출 후에는 다시 수정할 수 없다")
    void submittedResponseCannotBeUpdated() {
        PeerEvaluationSubmission submission = PeerEvaluationSubmission.create(1L, "20260001");
        LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 8, 12, 0);

        submission.update("기여", "평가", "회고", true, submittedAt);

        assertThat(submission.getStatus()).isEqualTo(PeerEvaluationSubmissionStatus.SUBMITTED);
        assertThat(submission.getSubmittedAt()).isEqualTo(submittedAt);
        assertThatThrownBy(() -> submission.update("수정", "수정", "수정", false, submittedAt.plusMinutes(1)))
            .isInstanceOf(PeerEvaluationAlreadySubmittedException.class);
    }

    @Test
    @DisplayName("팀원 기여도는 0 이상 100 이하만 허용한다")
    void contributionPercentRange() {
        assertThatThrownBy(() -> PeerEvaluationTeammateAnswer.create(1L, "20260002", -1, "기여", "평가"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PeerEvaluationTeammateAnswer.create(1L, "20260002", 101, "기여", "평가"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("팀원 기여도 경계값과 서술 답변 정규화를 지원한다")
    void normalizesTeammateAnswer() {
        PeerEvaluationTeammateAnswer minimum = PeerEvaluationTeammateAnswer.create(
            1L, "20260002", 0, null, "  개선할 점이 없습니다.  "
        );
        PeerEvaluationTeammateAnswer maximum = PeerEvaluationTeammateAnswer.create(
            1L, "20260003", 100, "  맡은 기능을 완료했습니다.  ", null
        );

        assertThat(minimum.getContributionPercent()).isZero();
        assertThat(minimum.getContributionDetail()).isEmpty();
        assertThat(minimum.getTeammateAssessment()).isEqualTo("개선할 점이 없습니다.");
        assertThat(maximum.getContributionPercent()).isEqualTo(100);
        assertThat(maximum.getContributionDetail()).isEqualTo("맡은 기능을 완료했습니다.");
        assertThat(maximum.getTeammateAssessment()).isEmpty();
    }

    @Test
    @DisplayName("팀원 평가 서술 답변은 2000자를 넘을 수 없다")
    void teammateAnswerLengthLimit() {
        String tooLong = "가".repeat(2001);

        assertThatThrownBy(() -> PeerEvaluationTeammateAnswer.create(1L, "20260002", 50, tooLong, "평가"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PeerEvaluationTeammateAnswer.create(1L, "20260002", 50, "기여", tooLong))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("상호평가 본문은 공백을 정리하고 null을 빈 문자열로 저장한다")
    void normalizesSubmissionAnswers() {
        PeerEvaluationSubmission submission = PeerEvaluationSubmission.create(1L, "20260001");

        submission.update("  본인 기여  ", null, "  회고  ", false, LocalDateTime.now());

        assertThat(submission.getSelfContribution()).isEqualTo("본인 기여");
        assertThat(submission.getProjectReviewComment()).isEmpty();
        assertThat(submission.getReflectionComment()).isEqualTo("회고");
        assertThat(submission.getStatus()).isEqualTo(PeerEvaluationSubmissionStatus.DRAFT);
        assertThat(submission.getSubmittedAt()).isNull();
    }

    @Test
    @DisplayName("상호평가 본문은 2000자를 넘을 수 없다")
    void submissionAnswerLengthLimit() {
        PeerEvaluationSubmission submission = PeerEvaluationSubmission.create(1L, "20260001");

        assertThatThrownBy(() -> submission.update("가".repeat(2001), "평가", "회고", false, LocalDateTime.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
