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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
            MeetingRecord.create(teamId, "회의록 제목", phase, "202412345", LocalDateTime.now(), "장소", "내용", List.of("202412345"))
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

    @Test
    @DisplayName("통합 조회는 회의 시간이 같으면 식별자 내림차순으로 안정 정렬한다")
    void getMeetingRecords_SameMeetingAt_OrdersByIdDescending() {
        LocalDateTime meetingAt = LocalDateTime.of(2026, 8, 25, 19, 30);
        MeetingRecord first = fakeMeetingRecordRepository.save(
            MeetingRecord.create(1L, "첫 회의록", MeetingPhase.MID_CHECK, "202412345", meetingAt, "온라인", "첫 회의", List.of())
        );
        MeetingRecord second = fakeMeetingRecordRepository.save(
            MeetingRecord.create(2L, "두 번째 회의록", MeetingPhase.MID_CHECK, "202412346", meetingAt, "온라인", "두 번째 회의", List.of())
        );

        var result = queryService.getMeetingRecords(
            List.of(1L, 2L),
            PageRequest.of(0, 20, Sort.by(Sort.Order.desc("meetingAt"), Sort.Order.desc("id")))
        );

        assertThat(result.getContent())
            .extracting(MeetingRecord::getId)
            .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("통합 조회는 Pageable의 정렬 조건을 반영한다")
    void getMeetingRecords_AppliesPageableSort() {
        MeetingRecord first = save(1L, MeetingPhase.PROPOSAL);
        MeetingRecord second = save(2L, MeetingPhase.FINAL);

        var result = queryService.getMeetingRecords(
            List.of(1L, 2L),
            PageRequest.of(0, 20, Sort.by(Sort.Order.asc("id")))
        );

        assertThat(result.getContent())
            .extracting(MeetingRecord::getId)
            .containsExactly(first.getId(), second.getId());
    }
}
