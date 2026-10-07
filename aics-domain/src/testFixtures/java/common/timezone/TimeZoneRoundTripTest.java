package common.timezone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordEditLogRepositoryImpl;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordJpaEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeAll;
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

// KD3-287 회귀 가드.
// 예전에는 JVM 기본 시간대(UTC)와 hibernate.jdbc.time_zone(Asia/Seoul)이 어긋나 Hibernate가
// 저장 시 +9, 조회 시 -9를 적용했다. 그 결과 DB에 적힌 값과 애플리케이션이 돌려주는 값이 9시간
// 달라졌고(감사 시각), 사용자가 넣은 값은 DB에만 9시간 미래로 저장됐다.
// 이 테스트는 "두 시간대가 같을 때 DB 저장값과 조회값이 일치한다"를 고정한다 —
// 누군가 jdbc.time_zone을 되살리거나 컨테이너 TZ 설정을 빼면 여기서 깨진다.
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

    @BeforeAll
    static void fixServiceTimeZone() {
        // 배포 컨테이너와 같은 조건(Dockerfile/compose의 TZ=Asia/Seoul)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    @Autowired
    private MeetingRecordEditLogRepositoryImpl editLogRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("감사 시각은 DB에 저장된 값과 애플리케이션이 돌려주는 값이 같다")
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
    @DisplayName("사용자가 입력한 시각은 그대로 저장된다(9시간 밀리지 않는다)")
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
