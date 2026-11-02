package teammessage.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import kgu.developers.domain.teammessage.domain.DocumentFeedbackSummary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DocumentFeedbackSummaryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 15, 0);

    @ParameterizedTest(name = "{0}")
    @MethodSource("requests")
    void detectsWaitingForProfessor(String description, LocalDateTime submittedAt,
            LocalDateTime feedbackAt, LocalDateTime studentAt, boolean expected) {
        var summary = new DocumentFeedbackSummary(feedbackAt == null ? 0 : 1, feedbackAt, studentAt);
        assertThat(summary.feedbackRequested(submittedAt)).isEqualTo(expected);
    }

    static Stream<Arguments> requests() {
        return Stream.of(
            Arguments.of("미제출·대화 없음", null, null, null, false),
            Arguments.of("첫 제출", NOW, null, null, true),
            Arguments.of("제출 후 교수 피드백", NOW, NOW.plusMinutes(1), null, false),
            Arguments.of("피드백 후 재제출", NOW.plusMinutes(2), NOW, null, true),
            Arguments.of("학생 답글 후 교수 답변 대기", null, NOW, NOW.plusMinutes(1), true),
            Arguments.of("학생 답글에 교수 답변 완료", null, NOW.plusMinutes(2), NOW, false),
            Arguments.of("학생 첫 요청", null, null, NOW, true),
            Arguments.of("같은 시각의 피드백으로 처리됨", NOW, NOW, NOW, false)
        );
    }
}
