package announcementpopup.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissal;
import kgu.developers.domain.announcementpopup.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(classes = StudentAnnouncementPopupRepositoryPostgresTest.TestApp.class)
@Import({StudentAnnouncementPopupRepositoryImpl.class, AnnouncementPopupDismissalRepositoryImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
class StudentAnnouncementPopupRepositoryPostgresTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 12, 0);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DB_URL");
        if (url == null) {
            PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15");
            postgres.start();
            url = postgres.getJdbcUrl();
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
        }
        String jdbcUrl = url;
        registry.add("spring.datasource.url", () -> jdbcUrl);
    }

    @SpringBootApplication
    @EntityScan(basePackages = "kgu.developers.domain")
    @EnableJpaRepositories(basePackageClasses = JpaStudentAnnouncementPopupRepository.class)
    static class TestApp { }

    @Autowired StudentAnnouncementPopupRepositoryImpl popups;
    @Autowired AnnouncementPopupDismissalRepositoryImpl dismissals;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE announcement_popup_dismissal, announcement_popup, section_announcement, enrollment, section, course, \"user\" CASCADE");
        jdbc.update("INSERT INTO \"user\" (student_number,email,name,password,global_role,phone,created_at,updated_at) VALUES ('prof','prof@example.test','교수','dummy','ADMIN','000',?,?)", NOW, NOW);
        jdbc.update("INSERT INTO course (id,name,year,semester,status,created_at,updated_at) VALUES (1,'강좌',2026,'FALL','ACTIVE',?,?)", NOW, NOW);
        jdbc.update("INSERT INTO section (id,professor_id,course_id,code,name,class_time,capacity,created_at,updated_at) VALUES (1,'prof',1,'S1','분반','월',30,?,?),(2,'prof',1,'S2','다른 분반','화',30,?,?)", NOW, NOW, NOW, NOW);
        jdbc.update("INSERT INTO enrollment (id,section_id,user_id,role,status,created_at,updated_at) VALUES (1,1,'student','STUDENT','ACTIVE',?,?)", NOW, NOW);
        for (long id = 1; id <= 3; id++) {
            long section = id == 3 ? 2 : 1;
            jdbc.update("INSERT INTO section_announcement (id,section_id,title,content,published_at,version,created_at,updated_at) VALUES (?,?,'제목','본문',?,0,?,?)", id, section, NOW, NOW, NOW);
            jdbc.update("INSERT INTO announcement_popup (id,announcement_id,section_id,title,content,enabled,version,content_version,created_at,updated_at) VALUES (?,?,?,'제목','내용',true,0,0,?,?)", id, id, section, NOW, NOW);
        }
    }

    @Test
    void publishedAtBoundaryAndSectionScope() {
        assertThat(popups.findAvailable("student", null, NOW)).extracting(p -> p.getId()).containsExactly(2L, 1L);
        assertThat(popups.findAvailable("student", 1L, NOW)).hasSize(1);
        assertThat(popups.findAvailable("student", 3L, NOW)).isEmpty();
        assertThat(popups.findAvailable("other", null, NOW)).isEmpty();
        assertThat(popups.findAvailable("prof", null, NOW)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "UPDATE enrollment SET role='ASSISTANT'",
        "UPDATE enrollment SET status='WITHDRAWN'",
        "UPDATE enrollment SET deleted_at=now()",
        "UPDATE section SET deleted_at=now()",
        "UPDATE course SET deleted_at=now()",
        "UPDATE course SET status='ARCHIVED'",
        "UPDATE section_announcement SET deleted_at=now()",
        "UPDATE section_announcement SET published_at='2026-11-26 12:00:01'",
        "UPDATE announcement_popup SET enabled=false",
        "UPDATE announcement_popup SET deleted_at=now()"
    })
    void inaccessiblePopupsExcluded(String sql) {
        jdbc.execute(sql);
        assertThat(popups.findAvailable("student", null, NOW)).isEmpty();
    }

    @Test
    void upsertCannotRegressVersionOrShortenSameVersion() {
        dismissals.save(new AnnouncementPopupDismissal("student", 1L, 1, NOW.plusDays(7)), NOW);
        AnnouncementPopupDismissal same = dismissals.save(
            new AnnouncementPopupDismissal("student", 1L, 1, NOW.plusDays(6)), NOW.minusDays(1));
        assertThat(same.hiddenUntil()).isEqualTo(NOW.plusDays(7));
        dismissals.save(new AnnouncementPopupDismissal("student", 1L, 2, NOW.plusDays(8)), NOW.plusDays(1));
        dismissals.save(new AnnouncementPopupDismissal("student", 1L, 1, NOW.plusDays(20)), NOW.plusDays(13));
        assertThat(dismissals.findAllByUserId("student")).containsExactly(
            new AnnouncementPopupDismissal("student", 1L, 2, NOW.plusDays(8)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM announcement_popup_dismissal", Long.class)).isEqualTo(1);
    }

    @Test
    void concurrentFirstDismissalsAndUserIsolation() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                final int offset = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try { go.await(10, TimeUnit.SECONDS); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                    dismissals.save(new AnnouncementPopupDismissal("student", 1L, 0, NOW.plusDays(7).plusSeconds(offset)), NOW.plusSeconds(offset));
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            for (Future<?> f : futures) f.get(20, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        assertThat(dismissals.findAllByUserId("student")).containsExactly(
            new AnnouncementPopupDismissal("student", 1L, 0, NOW.plusDays(7).plusSeconds(3)));
        assertThat(dismissals.findAllByUserId("other")).isEmpty();
        dismissals.save(new AnnouncementPopupDismissal("other", 1L, 0, NOW.plusDays(7)), NOW);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM announcement_popup_dismissal", Long.class)).isEqualTo(2);
    }

    @Test
    void ddlCanBeAppliedTwice() throws Exception {
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of("../database/announcement_popup_dismissal.sql"));
        jdbc.execute(sql);
        jdbc.execute(sql);
        dismissals.save(new AnnouncementPopupDismissal("student", 1L, 0, NOW.plusDays(7)), NOW);
        assertThat(dismissals.findAllByUserId("student")).hasSize(1);
    }
}
