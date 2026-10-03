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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
            MeetingRecord.create(teamId, "회의록 제목", MeetingPhase.PROPOSAL, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345"))
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

    @Test
    @DisplayName("getSectionActions는 지정한 팀 목록의 액션플랜만 반환한다")
    void getSectionActions_FiltersByTeamIds() {
        // given
        MeetingRecord firstTeamRecord = createMeetingRecord(1L);
        MeetingRecord secondTeamRecord = createMeetingRecord(2L);
        MeetingRecord outsideRecord = createMeetingRecord(3L);
        saveAt(firstTeamRecord.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 10, 0));
        saveAt(secondTeamRecord.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 11, 0));
        saveAt(outsideRecord.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 12, 0));

        // when
        Page<MeetingAction> results = queryService.getSectionActions(
            List.of(1L, 2L), null, null, latestFirst(0, 20));

        // then
        assertThat(results.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("getSectionActions는 생성 최신순으로 정렬하고 같은 시각이면 식별자 역순으로 정렬한다")
    void getSectionActions_SortsByCreatedAtThenId() {
        // given
        MeetingRecord record = createMeetingRecord(1L);
        LocalDateTime sameMoment = LocalDateTime.of(2026, 10, 1, 10, 0);
        MeetingAction older = saveAt(record.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 9, 30, 10, 0));
        MeetingAction sameFirst = saveAt(record.getId(), MeetingActionStatus.TODO, sameMoment);
        MeetingAction sameSecond = saveAt(record.getId(), MeetingActionStatus.TODO, sameMoment);

        // when
        Page<MeetingAction> results = queryService.getSectionActions(
            List.of(1L), null, null, latestFirst(0, 20));

        // then
        assertThat(results.getContent())
            .extracting(MeetingAction::getId)
            .containsExactly(sameSecond.getId(), sameFirst.getId(), older.getId());
    }

    @Test
    @DisplayName("getSectionActions는 회의록·상태 필터를 적용한다")
    void getSectionActions_FiltersByMeetingRecordAndStatus() {
        // given
        MeetingRecord target = createMeetingRecord(1L);
        MeetingRecord other = createMeetingRecord(1L);
        saveAt(target.getId(), MeetingActionStatus.DONE, LocalDateTime.of(2026, 10, 1, 10, 0));
        saveAt(target.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 11, 0));
        saveAt(other.getId(), MeetingActionStatus.DONE, LocalDateTime.of(2026, 10, 1, 12, 0));

        // when
        Page<MeetingAction> results = queryService.getSectionActions(
            List.of(1L), target.getId(), MeetingActionStatus.DONE, latestFirst(0, 20));

        // then
        assertThat(results.getContent()).singleElement().satisfies(action -> {
            assertThat(action.getMeetingRecordId()).isEqualTo(target.getId());
            assertThat(action.getStatus()).isEqualTo(MeetingActionStatus.DONE);
        });
    }

    @Test
    @DisplayName("getSectionActions는 페이지 크기를 넘는 결과를 나눠서 반환한다")
    void getSectionActions_Paginates() {
        // given
        MeetingRecord record = createMeetingRecord(1L);
        saveAt(record.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 10, 0));
        MeetingAction newer = saveAt(record.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 11, 0));

        // when
        Page<MeetingAction> firstPage = queryService.getSectionActions(
            List.of(1L), null, null, latestFirst(0, 1));

        // then
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.isLast()).isFalse();
        assertThat(firstPage.getContent()).extracting(MeetingAction::getId).containsExactly(newer.getId());
    }

    @Test
    @DisplayName("getSectionActions는 팀 목록이 비어 있으면 빈 페이지를 반환한다")
    void getSectionActions_EmptyTeamIds() {
        // given
        MeetingRecord record = createMeetingRecord(1L);
        saveAt(record.getId(), MeetingActionStatus.TODO, LocalDateTime.of(2026, 10, 1, 10, 0));

        // when
        Page<MeetingAction> results = queryService.getSectionActions(List.of(), null, null, latestFirst(0, 20));

        // then
        assertThat(results.getTotalElements()).isZero();
    }

    private MeetingAction saveAt(Long meetingRecordId, MeetingActionStatus status, LocalDateTime createdAt) {
        return fakeMeetingActionRepository.save(MeetingAction.builder()
            .meetingRecordId(meetingRecordId)
            .assigneeId("202412345")
            .content("내용")
            .status(status)
            .createdAt(createdAt)
            .build());
    }

    private Pageable latestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
}
