package meetingrecord.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordInvalidEditReasonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingRecordEditLogTest {

    private static final int MIN = MeetingRecordEditLog.MIN_REASON_LENGTH;
    private static final int MAX = MeetingRecordEditLog.MAX_REASON_LENGTH;

    @Test
    @DisplayName("사유가 정확히 30자면 통과한다")
    void create_MinLengthBoundary() {
        assertThatCode(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", "가".repeat(MIN)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사유가 29자면 거부한다")
    void create_BelowMinLength() {
        assertThatThrownBy(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", "가".repeat(MIN - 1)))
            .isInstanceOf(MeetingRecordInvalidEditReasonException.class);
    }

    @Test
    @DisplayName("사유가 정확히 500자면 통과한다")
    void create_MaxLengthBoundary() {
        assertThatCode(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", "가".repeat(MAX)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사유가 501자면 거부한다")
    void create_AboveMaxLength() {
        assertThatThrownBy(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", "가".repeat(MAX + 1)))
            .isInstanceOf(MeetingRecordInvalidEditReasonException.class);
    }

    @Test
    @DisplayName("앞뒤 공백으로 글자 수를 채운 사유는 트림 후 길이로 판정해 거부한다")
    void create_PaddedWithWhitespace() {
        String padded = " ".repeat(20) + "가".repeat(MIN - 1) + " ".repeat(20);

        assertThatThrownBy(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", padded))
            .isInstanceOf(MeetingRecordInvalidEditReasonException.class);
    }

    @Test
    @DisplayName("앞뒤 공백 때문에 상한을 넘긴 사유는 트림 후 길이로 판정해 통과하고 트림된 값으로 저장된다")
    void create_TrimsBeforeLengthCheck() {
        String padded = "  " + "가".repeat(MAX) + "  ";

        MeetingRecordEditLog editLog = MeetingRecordEditLog.create(1L, 10L, "202412345", padded);

        assertThat(editLog.getReason()).isEqualTo("가".repeat(MAX));
        assertThat(editLog.getReason()).hasSize(MAX);
    }

    @Test
    @DisplayName("사유가 null이면 거부한다")
    void create_NullReason() {
        assertThatThrownBy(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", null))
            .isInstanceOf(MeetingRecordInvalidEditReasonException.class);
    }

    @Test
    @DisplayName("공백만 있는 사유는 거부한다")
    void create_BlankReason() {
        assertThatThrownBy(() -> MeetingRecordEditLog.create(1L, 10L, "202412345", "   "))
            .isInstanceOf(MeetingRecordInvalidEditReasonException.class);
    }

    @Test
    @DisplayName("회의록·팀·수정자 정보를 그대로 담는다")
    void create_KeepsIdentifiers() {
        MeetingRecordEditLog editLog = MeetingRecordEditLog.create(7L, 20L, "202412345", "가".repeat(MIN));

        assertThat(editLog.getMeetingRecordId()).isEqualTo(7L);
        assertThat(editLog.getTeamId()).isEqualTo(20L);
        assertThat(editLog.getEditorId()).isEqualTo("202412345");
    }
}
