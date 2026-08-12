package meetingrecord.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingActionTest {

    @Test
    @DisplayName("create는 담당자 없이도 액션플랜을 생성한다")
    void create_WithoutAssignee() {
        // when
        MeetingAction meetingAction = MeetingAction.create(1L, null, "내용", MeetingActionStatus.IN_PROGRESS, null);

        // then
        assertThat(meetingAction.getMeetingRecordId()).isEqualTo(1L);
        assertThat(meetingAction.getAssigneeId()).isNull();
        assertThat(meetingAction.getContent()).isEqualTo("내용");
        assertThat(meetingAction.getStatus()).isEqualTo(MeetingActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("update 메서드들은 각 필드를 갱신한다")
    void updaters_UpdateFields() {
        // given
        MeetingAction meetingAction = MeetingAction.create(1L, "202412345", "내용", MeetingActionStatus.IN_PROGRESS, null);
        LocalDateTime dueAt = LocalDateTime.of(2026, 9, 1, 0, 0);

        // when
        meetingAction.updateContent("수정된 내용");
        meetingAction.updateStatus(MeetingActionStatus.DONE);
        meetingAction.updateDueAt(dueAt);
        meetingAction.updateAssigneeId("202499999");

        // then
        assertThat(meetingAction.getContent()).isEqualTo("수정된 내용");
        assertThat(meetingAction.getStatus()).isEqualTo(MeetingActionStatus.DONE);
        assertThat(meetingAction.getDueAt()).isEqualTo(dueAt);
        assertThat(meetingAction.getAssigneeId()).isEqualTo("202499999");
    }
}
