package meetingrecord.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import mock.repository.FakeMeetingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingRecordQueryServiceTest {

    private FakeMeetingRecordRepository fakeMeetingRecordRepository;
    private MeetingRecordQueryService queryService;

    @BeforeEach
    void init() {
        fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        queryService = new MeetingRecordQueryService(fakeMeetingRecordRepository);
    }

    private MeetingRecord save(Long teamId, MeetingPhase phase) {
        return fakeMeetingRecordRepository.save(
            MeetingRecord.create(teamId, phase, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345"))
        );
    }

    @Test
    @DisplayName("getMeetingRecord는 id로 회의록을 조회한다")
    void getMeetingRecord_Success() {
        // given
        MeetingRecord saved = save(1L, MeetingPhase.PROPOSAL);

        // when
        MeetingRecord found = queryService.getMeetingRecord(saved.getId());

        // then
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("getMeetingRecord는 존재하지 않는 id면 예외를 던진다")
    void getMeetingRecord_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> queryService.getMeetingRecord(999L))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getMeetingRecords는 phase 없이 조회하면 팀의 회의록 전체를 반환한다")
    void getMeetingRecords_WithoutPhase_ReturnsAll() {
        // given
        save(1L, MeetingPhase.PROPOSAL);
        save(1L, MeetingPhase.FINAL);

        // when
        List<MeetingRecord> results = queryService.getMeetingRecords(1L, null);

        // then
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("getMeetingRecords는 phase로 필터링한 목록을 반환한다")
    void getMeetingRecords_FilterByPhase() {
        // given
        save(1L, MeetingPhase.PROPOSAL);
        save(1L, MeetingPhase.FINAL);

        // when
        List<MeetingRecord> results = queryService.getMeetingRecords(1L, MeetingPhase.FINAL);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPhase()).isEqualTo(MeetingPhase.FINAL);
    }
}
