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
}
