package meetingrecord.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.command.MeetingRecordCommandService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import mock.repository.FakeMeetingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingRecordCommandServiceTest {

    private FakeMeetingRecordRepository fakeMeetingRecordRepository;
    private MeetingRecordCommandService commandService;

    @BeforeEach
    void init() {
        fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        commandService = new MeetingRecordCommandService(fakeMeetingRecordRepository);
    }

    private Long createMeetingRecord() {
        return commandService.createMeetingRecord(
            1L, MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345")
        );
    }

    @Test
    @DisplayName("createMeetingRecord는 저장된 회의록의 id를 반환한다")
    void createMeetingRecord_ReturnsSavedId() {
        // when
        Long id = createMeetingRecord();

        // then
        assertThat(id).isNotNull();
        MeetingRecord saved = fakeMeetingRecordRepository.findById(id).orElseThrow();
        assertThat(saved.getAuthorId()).isEqualTo("202412345");
    }

    @Test
    @DisplayName("updateMeetingRecord는 전달된 필드만 갱신한다")
    void updateMeetingRecord_UpdatesOnlyProvidedFields() {
        // given
        Long id = createMeetingRecord();

        // when
        commandService.updateMeetingRecord(id, null, "새 장소", MeetingPhase.FINAL, "새 내용", null);

        // then
        MeetingRecord updated = fakeMeetingRecordRepository.findById(id).orElseThrow();
        assertThat(updated.getLocation()).isEqualTo("새 장소");
        assertThat(updated.getPhase()).isEqualTo(MeetingPhase.FINAL);
        assertThat(updated.getContent()).isEqualTo("새 내용");
        assertThat(updated.getParticipantCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateMeetingRecord는 존재하지 않는 회의록이면 예외를 던진다")
    void updateMeetingRecord_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingRecord(999L, null, null, null, "내용", null))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateMeetingRecord는 공백만 있는 content로 수정하면 예외를 던진다")
    void updateMeetingRecord_BlankContent_ThrowsException() {
        // given
        Long id = createMeetingRecord();

        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingRecord(id, null, null, null, "   ", null))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("deleteMeetingRecord는 회의록을 삭제한다")
    void deleteMeetingRecord_Success() {
        // given
        Long id = createMeetingRecord();

        // when
        commandService.deleteMeetingRecord(id);

        // then
        assertThat(fakeMeetingRecordRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("deleteMeetingRecord는 존재하지 않는 회의록이면 예외를 던진다")
    void deleteMeetingRecord_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commandService.deleteMeetingRecord(999L))
            .isInstanceOf(CustomException.class);
    }
}
