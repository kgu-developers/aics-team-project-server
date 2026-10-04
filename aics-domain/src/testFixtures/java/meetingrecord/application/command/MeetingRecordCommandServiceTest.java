package meetingrecord.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.command.MeetingRecordCommandService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import mock.repository.FakeMeetingRecordEditLogRepository;
import mock.repository.FakeMeetingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingRecordCommandServiceTest {

    private FakeMeetingRecordRepository fakeMeetingRecordRepository;
    private FakeMeetingRecordEditLogRepository fakeMeetingRecordEditLogRepository;
    private MeetingRecordCommandService commandService;

    @BeforeEach
    void init() {
        fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        fakeMeetingRecordEditLogRepository = new FakeMeetingRecordEditLogRepository();
        commandService = new MeetingRecordCommandService(
            fakeMeetingRecordRepository, fakeMeetingRecordEditLogRepository);
    }

    private Long createMeetingRecord() {
        return commandService.createMeetingRecord(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345")
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
        commandService.updateMeetingRecord(id, null, null, "새 장소", MeetingPhase.FINAL, "새 내용", null);

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
        assertThatThrownBy(() -> commandService.updateMeetingRecord(999L, null, null, null, null, "내용", null))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateMeetingRecord는 공백만 있는 content로 수정하면 예외를 던진다")
    void updateMeetingRecord_BlankContent_ThrowsException() {
        // given
        Long id = createMeetingRecord();

        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingRecord(id, null, null, null, null, "   ", null))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("수정 사유를 주면 같은 수정에서 수정 이력이 한 건 적재된다")
    void updateMeetingRecord_RecordsEditLog() {
        // given
        Long id = createMeetingRecord();
        String reason = "회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 수정했습니다.";

        // when
        commandService.updateMeetingRecord(
            id, null, null, null, null, "새 내용", null, null, reason, "202412345");

        // then
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).singleElement().satisfies(log -> {
            assertThat(log.getMeetingRecordId()).isEqualTo(id);
            assertThat(log.getTeamId()).isEqualTo(1L);
            assertThat(log.getEditorId()).isEqualTo("202412345");
            assertThat(log.getReason()).isEqualTo(reason);
        });
    }

    @Test
    @DisplayName("수정할 때마다 이력이 덮어쓰이지 않고 계속 쌓인다")
    void updateMeetingRecord_AppendsEditLog() {
        // given
        Long id = createMeetingRecord();

        // when
        commandService.updateMeetingRecord(
            id, null, null, null, null, "첫 번째 수정", null, null, "가".repeat(30), "202412345");
        commandService.updateMeetingRecord(
            id, null, null, null, null, "두 번째 수정", null, null, "나".repeat(30), "202412346");

        // then
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).hasSize(2)
            .extracting(log -> log.getEditorId())
            .containsExactly("202412345", "202412346");
    }

    @Test
    @DisplayName("사유가 30자 미만이면 예외를 던지고 이력도 남기지 않는다")
    void updateMeetingRecord_TooShortReason_ThrowsException() {
        // given
        Long id = createMeetingRecord();

        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingRecord(
            id, null, null, null, null, "새 내용", null, null, "짧은 사유", "202412345"))
            .isInstanceOf(CustomException.class);
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("수정할 필드 없이 사유만 보내면 거부하고 이력도 남기지 않는다")
    void updateMeetingRecord_ReasonOnly_ThrowsException() {
        // given
        Long id = createMeetingRecord();

        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingRecord(
            id, null, null, null, null, null, null, null, "가".repeat(30), "202412345"))
            .isInstanceOf(CustomException.class);
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("같은 값을 다시 보내면 실제 변경이 없으므로 이력을 남기지 않는다")
    void updateMeetingRecord_SameValue_DoesNotRecordLog() {
        // given
        Long id = createMeetingRecord();
        MeetingRecord before = fakeMeetingRecordRepository.findById(id).orElseThrow();

        // when
        commandService.updateMeetingRecord(
            id, before.getTitle(), before.getMeetingAt(), before.getLocation(), before.getPhase(),
            before.getContent(), List.of("202412345"), null, "가".repeat(30), "202412345");

        // then
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("한 필드라도 실제로 바뀌면 이력을 남긴다")
    void updateMeetingRecord_ChangedValue_RecordsLog() {
        // given
        Long id = createMeetingRecord();
        MeetingRecord before = fakeMeetingRecordRepository.findById(id).orElseThrow();

        // when
        commandService.updateMeetingRecord(
            id, before.getTitle(), null, null, null, "바뀐 내용", null, null, "가".repeat(30), "202412345");

        // then
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("참석자 순서만 바꿔 보내면 실제 연결이 같으므로 이력을 남기지 않는다")
    void updateMeetingRecord_ReorderedParticipants_DoesNotRecordLog() {
        // given — 참석자 두 명으로 회의록 생성
        Long id = commandService.createMeetingRecord(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용",
            List.of("202412345", "202412346"));

        // when — 같은 두 명을 순서만 뒤집어 전달
        commandService.updateMeetingRecord(
            id, null, null, null, null, null, List.of("202412346", "202412345"), null,
            "가".repeat(30), "202412345");

        // then — 리포지토리 동기화가 집합 기준이라 실제 변경이 없다
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("마일스톤 순서만 바꿔 보내면 이력을 남기지 않는다")
    void updateMeetingRecord_ReorderedMilestones_DoesNotRecordLog() {
        // given
        Long id = commandService.createMeetingRecord(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용",
            List.of("202412345"), List.of(3L, 4L));

        // when
        commandService.updateMeetingRecord(
            id, null, null, null, null, null, null, List.of(4L, 3L), "가".repeat(30), "202412345");

        // then
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("참석자가 실제로 바뀌면 이력을 남긴다")
    void updateMeetingRecord_ChangedParticipants_RecordsLog() {
        // given
        Long id = commandService.createMeetingRecord(
            1L, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용",
            List.of("202412345", "202412346"));

        // when — 한 명을 뺀다
        commandService.updateMeetingRecord(
            id, null, null, null, null, null, List.of("202412345"), null, "가".repeat(30), "202412345");

        // then
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("변경이 없는 요청이라도 사유가 30자 미만이면 거부한다")
    void updateMeetingRecord_NoChangeWithTooShortReason_ThrowsException() {
        // given
        Long id = createMeetingRecord();
        MeetingRecord before = fakeMeetingRecordRepository.findById(id).orElseThrow();

        // when & then — 같은 값을 보내더라도 사유 규칙은 동일하게 적용된다
        assertThatThrownBy(() -> commandService.updateMeetingRecord(
            id, before.getTitle(), null, null, null, null, null, null, "짧은 사유", "202412345"))
            .isInstanceOf(CustomException.class);
        assertThat(fakeMeetingRecordEditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("변경이 없는 요청이라도 사유가 500자를 넘으면 거부한다")
    void updateMeetingRecord_NoChangeWithTooLongReason_ThrowsException() {
        // given
        Long id = createMeetingRecord();
        MeetingRecord before = fakeMeetingRecordRepository.findById(id).orElseThrow();

        // when & then
        assertThatThrownBy(() -> commandService.updateMeetingRecord(
            id, before.getTitle(), null, null, null, null, null, null, "가".repeat(501), "202412345"))
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
