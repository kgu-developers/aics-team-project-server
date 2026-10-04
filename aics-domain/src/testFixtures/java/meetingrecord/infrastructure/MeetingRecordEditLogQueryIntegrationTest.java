package meetingrecord.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordEditLogRepositoryImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 수정 이력 조회는 선택 필터(null 허용)와 기준일 집계를 실제 SQL로 돌리므로,
// Fake로는 못 잡는 JPQL·정렬 별칭·경계 조건을 H2에 직접 태워 확인한다.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:meeting-record-edit-log;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(MeetingRecordEditLogRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MeetingRecordEditLogQueryIntegrationTest {

    private static final String REASON = "회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 고쳤습니다.";
    private static final Pageable LATEST_FIRST =
        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.meetingrecord.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.meetingrecord.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private MeetingRecordEditLogRepositoryImpl repository;

    @Test
    @DisplayName("회의록별 조회는 최신순으로 쌓인 이력을 돌려준다")
    void findAllByMeetingRecordId_LatestFirst() {
        MeetingRecordEditLog first = save(1L, 10L, "202412345");
        MeetingRecordEditLog second = save(1L, 10L, "202412346");
        save(2L, 10L, "202412347");

        Page<MeetingRecordEditLog> result = repository.findAllByMeetingRecordId(1L, LATEST_FIRST);

        Assertions.assertThat(result.getContent())
            .extracting(MeetingRecordEditLog::getId)
            .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("분반 조회는 팀 목록으로 거르고 회의록 필터가 없으면 팀 전체 이력을 돌려준다")
    void findAllByTeamIdIn_WithoutMeetingRecordFilter() {
        save(1L, 10L, "202412345");
        save(2L, 20L, "202412346");
        save(3L, 99L, "202412347");

        Page<MeetingRecordEditLog> result = repository.findAllByTeamIdIn(List.of(10L, 20L), null, LATEST_FIRST);

        Assertions.assertThat(result.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(result.getContent())
            .extracting(MeetingRecordEditLog::getTeamId)
            .containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("분반 조회에 회의록 필터를 주면 그 회의록 이력만 돌려준다")
    void findAllByTeamIdIn_WithMeetingRecordFilter() {
        MeetingRecordEditLog target = save(1L, 10L, "202412345");
        save(2L, 10L, "202412346");

        Page<MeetingRecordEditLog> result = repository.findAllByTeamIdIn(List.of(10L), 1L, LATEST_FIRST);

        Assertions.assertThat(result.getContent())
            .extracting(MeetingRecordEditLog::getId)
            .containsExactly(target.getId());
    }

    @Test
    @DisplayName("기준일 이전 이력만 팀별로 집계한다")
    void countByTeamIdInUntil_CountsPerTeam() {
        save(1L, 10L, "202412345");
        save(1L, 10L, "202412346");
        save(2L, 20L, "202412347");

        Map<Long, Long> counts = repository.countByTeamIdInUntil(
            List.of(10L, 20L), LocalDateTime.now().plusMinutes(1));

        Assertions.assertThat(counts).containsEntry(10L, 2L).containsEntry(20L, 1L);
    }

    @Test
    @DisplayName("기준일보다 뒤에 쌓인 이력은 집계에서 뺀다")
    void countByTeamIdInUntil_ExcludesAfterUntil() {
        save(1L, 10L, "202412345");

        Map<Long, Long> counts = repository.countByTeamIdInUntil(
            List.of(10L), LocalDateTime.now().minusDays(1));

        Assertions.assertThat(counts).isEmpty();
    }

    @Test
    @DisplayName("회의록이 삭제되면 그 회의록의 이력만 함께 지운다")
    void deleteAllByMeetingRecordId_RemovesOnlyThatRecordLogs() {
        save(1L, 10L, "202412345");
        MeetingRecordEditLog survivor = save(2L, 10L, "202412346");

        repository.deleteAllByMeetingRecordId(1L);

        Page<MeetingRecordEditLog> remaining = repository.findAllByTeamIdIn(List.of(10L), null, LATEST_FIRST);
        Assertions.assertThat(remaining.getContent())
            .extracting(MeetingRecordEditLog::getId)
            .containsExactly(survivor.getId());
    }

    private MeetingRecordEditLog save(Long meetingRecordId, Long teamId, String editorId) {
        return repository.save(MeetingRecordEditLog.create(meetingRecordId, teamId, editorId, REASON));
    }
}
