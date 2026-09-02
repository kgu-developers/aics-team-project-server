package meetingrecord.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.query.MeetingActionQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import mock.repository.FakeMeetingActionRepository;
import mock.repository.FakeMeetingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingActionQueryServiceTest {

    private FakeMeetingRecordRepository fakeMeetingRecordRepository;
    private FakeMeetingActionRepository fakeMeetingActionRepository;
    private MeetingActionQueryService queryService;

    @BeforeEach
    void init() {
        fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        fakeMeetingActionRepository = new FakeMeetingActionRepository(fakeMeetingRecordRepository);
        queryService = new MeetingActionQueryService(fakeMeetingActionRepository);
    }

    private MeetingAction save(Long meetingRecordId, MeetingActionStatus status) {
        return fakeMeetingActionRepository.save(
            MeetingAction.create(meetingRecordId, "202412345", "내용", status, null)
        );
    }

    private MeetingRecord createMeetingRecord(Long teamId) {
        return fakeMeetingRecordRepository.save(
            MeetingRecord.create(teamId, MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345"))
        );
    }

    @Test
    @DisplayName("getMeetingAction은 id로 액션플랜을 조회한다")
    void getMeetingAction_Success() {
        // given
        MeetingAction saved = save(1L, MeetingActionStatus.IN_PROGRESS);

        // when
        MeetingAction found = queryService.getMeetingAction(saved.getId());

        // then
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("getMeetingAction은 존재하지 않는 id면 예외를 던진다")
    void getMeetingAction_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> queryService.getMeetingAction(999L))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getMeetingActions는 회의록에 속한 액션플랜 전체를 반환한다")
    void getMeetingActions_ReturnsAll() {
        // given
        save(1L, MeetingActionStatus.IN_PROGRESS);
        save(1L, MeetingActionStatus.DONE);
        save(2L, MeetingActionStatus.IN_PROGRESS);

        // when
        List<MeetingAction> results = queryService.getMeetingActions(1L);

        // then
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("getTeamActions는 팀 전체 액션플랜을 status로 필터링해 반환한다")
    void getTeamActions_FilterByStatus() {
        // given
        MeetingRecord record1 = createMeetingRecord(1L);
        MeetingRecord record2 = createMeetingRecord(1L);
        save(record1.getId(), MeetingActionStatus.IN_PROGRESS);
        save(record2.getId(), MeetingActionStatus.DONE);

        // when
        List<MeetingAction> results = queryService.getTeamActions(1L, MeetingActionStatus.DONE);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(MeetingActionStatus.DONE);
    }

    @Test
    @DisplayName("getTeamActions는 status 없이 조회하면 팀 전체 액션플랜을 반환한다")
    void getTeamActions_WithoutStatus_ReturnsAll() {
        // given
        MeetingRecord record1 = createMeetingRecord(1L);
        save(record1.getId(), MeetingActionStatus.IN_PROGRESS);
        save(record1.getId(), MeetingActionStatus.DONE);

        // when
        List<MeetingAction> results = queryService.getTeamActions(1L, null);

        // then
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("getTeamActions는 다른 팀의 액션플랜은 반환하지 않는다")
    void getTeamActions_ExcludesOtherTeams() {
        // given
        MeetingRecord ourTeamRecord = createMeetingRecord(1L);
        MeetingRecord otherTeamRecord = createMeetingRecord(2L);
        save(ourTeamRecord.getId(), MeetingActionStatus.IN_PROGRESS);
        save(otherTeamRecord.getId(), MeetingActionStatus.IN_PROGRESS);

        // when
        List<MeetingAction> results = queryService.getTeamActions(1L, null);

        // then
        assertThat(results).hasSize(1);
    }
}
