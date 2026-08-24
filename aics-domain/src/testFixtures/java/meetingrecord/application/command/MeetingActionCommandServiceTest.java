package meetingrecord.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.command.MeetingActionCommandService;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import mock.repository.FakeMeetingActionRepository;
import mock.repository.FakeMeetingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingActionCommandServiceTest {

    private FakeMeetingActionRepository fakeMeetingActionRepository;
    private MeetingActionCommandService commandService;

    @BeforeEach
    void init() {
        fakeMeetingActionRepository = new FakeMeetingActionRepository(new FakeMeetingRecordRepository());
        commandService = new MeetingActionCommandService(fakeMeetingActionRepository);
    }

    private Long createMeetingAction() {
        return commandService.createMeetingAction(
            1L, "202412345", "내용", MeetingActionStatus.IN_PROGRESS, LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("createMeetingAction은 저장된 액션플랜의 id를 반환한다")
    void createMeetingAction_ReturnsSavedId() {
        // when
        Long id = createMeetingAction();

        // then
        assertThat(id).isNotNull();
        MeetingAction saved = fakeMeetingActionRepository.findById(id).orElseThrow();
        assertThat(saved.getContent()).isEqualTo("내용");
        assertThat(saved.getAssigneeId()).isEqualTo("202412345");
    }

    @Test
    @DisplayName("updateMeetingAction은 전달된 필드만 갱신한다")
    void updateMeetingAction_UpdatesOnlyProvidedFields() {
        // given
        Long id = createMeetingAction();

        // when
        commandService.updateMeetingAction(id, null, "수정된 내용", MeetingActionStatus.DONE, null, false, false);

        // then
        MeetingAction updated = fakeMeetingActionRepository.findById(id).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("수정된 내용");
        assertThat(updated.getStatus()).isEqualTo(MeetingActionStatus.DONE);
        assertThat(updated.getAssigneeId()).isEqualTo("202412345");
    }

    @Test
    @DisplayName("updateMeetingAction은 clearAssignee가 true면 assigneeId 값과 무관하게 담당자를 해제한다")
    void updateMeetingAction_ClearAssignee_RemovesAssignee() {
        // given
        Long id = createMeetingAction();

        // when
        commandService.updateMeetingAction(id, "202412399", null, null, null, true, false);

        // then
        MeetingAction updated = fakeMeetingActionRepository.findById(id).orElseThrow();
        assertThat(updated.getAssigneeId()).isNull();
    }

    @Test
    @DisplayName("updateMeetingAction은 clearDueAt이 true면 마감일을 해제한다")
    void updateMeetingAction_ClearDueAt_RemovesDueAt() {
        // given
        Long id = createMeetingAction();

        // when
        commandService.updateMeetingAction(id, null, null, null, null, false, true);

        // then
        MeetingAction updated = fakeMeetingActionRepository.findById(id).orElseThrow();
        assertThat(updated.getDueAt()).isNull();
    }

    @Test
    @DisplayName("updateMeetingAction은 공백만 있는 content로 수정하면 예외를 던진다")
    void updateMeetingAction_BlankContent_ThrowsException() {
        // given
        Long id = createMeetingAction();

        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingAction(id, null, "   ", null, null, false, false))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateMeetingAction은 존재하지 않는 액션플랜이면 예외를 던진다")
    void updateMeetingAction_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingAction(999L, null, "내용", null, null, false, false))
            .isInstanceOf(CustomException.class);
    }
}
