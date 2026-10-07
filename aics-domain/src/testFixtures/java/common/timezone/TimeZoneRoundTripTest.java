package common.timezone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordEditLogRepositoryImpl;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordJpaEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

// KD3-287 — "변환 설정(jdbc.time_zone)이 없으면 저장값과 조회값이 어긋나지 않는다"를 고정한다.
//
// JVM 기본 시간대는 일부러 건드리지 않는다. 변환이 없으면 어느 시간대에서 돌든 왕복이 성립하기
// 때문이고, 테스트가 전역 시간대를 바꾸면 같은 JVM에서 도는 다른 테스트가 영향을 받는다
// (실제로 @AfterAll 복구를 넣었다가 MeetingRecordEditLogQueryIntegrationTest가 깨졌다).
//
// 배포 설정이 잘못된 것(컨테이너 TZ 누락, jdbc.time_zone 재추가)은 이 테스트가 아니라
// TimeZoneConfigurationTest가 설정 파일을 직접 읽어 잡는다.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:tz-roundtrip;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(MeetingRecordEditLogRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TimeZoneRoundTripTest {

    private static final String REASON = "회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 고쳤습니다.";

    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.meetingrecord.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.meetingrecord.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private MeetingRecordEditLogRepositoryImpl editLogRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("변환 설정이 없으면 감사 시각의 DB 저장값과 조회값이 일치한다")
    void auditTimestampRoundTripsWithoutShift() {
        MeetingRecordEditLog saved = editLogRepository.save(
                MeetingRecordEditLog.create(1L, 10L, "202412345", REASON));

        LocalDateTime fromApplication = editLogRepository
                .findAllByMeetingRecordId(1L, PageRequest.of(0, 1))
                .getContent().get(0).getCreatedAt();
        String fromDatabase = jdbc.queryForObject(
                "select cast(created_at as varchar) from \"meeting_record_edit_log\" where id = ?",
                String.class, saved.getId());

        // DB는 "2026-10-07 11:39:05", LocalDateTime#toString은 "2026-10-07T11:39:05" 형태라
        // 구분자만 맞춘 뒤 같은 시각인지 비교한다(9시간 밀리면 여기서 깨진다).
        assertThat(LocalDateTime.parse(fromDatabase.replace(' ', 'T')))
                .isCloseTo(fromApplication, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("변환 설정이 없으면 사용자가 입력한 시각이 그대로 저장된다")
    void userSuppliedTimestampIsStoredAsIs() {
        LocalDateTime inputByUser = LocalDateTime.of(2026, 10, 15, 12, 0);

        em.persist(MeetingRecordJpaEntity.builder()
                .teamId(1L).title("회의").phase(MeetingPhase.PROPOSAL).authorId("202412345")
                .meetingAt(inputByUser).content("내용").build());
        em.flush();

        String stored = jdbc.queryForObject(
                "select cast(meeting_at as varchar) from \"meeting_record\" where team_id = 1", String.class);

        assertThat(stored).startsWith("2026-10-15 12:00");
    }
}
