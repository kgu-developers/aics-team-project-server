package topiccandidate.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.team.infrastructure.TeamRepositoryImpl;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberRepositoryImpl;
import kgu.developers.domain.topicCandidate.application.command.TopicCandidateCommandService;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateException;
import kgu.developers.domain.topicCandidate.infrastructure.TopicCandidateRepositoryImpl;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 동일 사용자의 주제 후보 동시 등록 및 제약 위반 시 409 변환을
 * 실제 PostgreSQL DB 환경(Testcontainers)에서 검증한다.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TopicCandidateConcurrencyTest {

    private static final String PROPOSER_USER_ID = "202412345";
    private static final String OTHER_PROPOSER_USER_ID = "202412346";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, SecurityAutoConfiguration.class})
    @EntityScan("kgu.developers.domain")
    @EnableJpaRepositories("kgu.developers.domain")
    @Import({
        TopicCandidateCommandService.class,
        TopicCandidateRepositoryImpl.class,
        TeamRepositoryImpl.class,
        TeamMemberRepositoryImpl.class
    })
    static class TestConfig {
    }

    @Autowired
    private TopicCandidateCommandService topicCandidateCommandService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long teamId;

    @BeforeEach
    void seed() {
        applyPartialUniqueIndexes();
        jdbcTemplate.update("DELETE FROM topic_vote");
        jdbcTemplate.update("DELETE FROM topic_candidate");
        transactionTemplate.executeWithoutResult(status -> {
            UserJpaEntity professor = UserJpaEntity.builder()
                .studentNumber("P" + System.nanoTime() % 1_000_000)
                .email("professor" + System.nanoTime() + "@kgu.ac.kr")
                .name("교수")
                .password("password")
                .globalRole(UserGlobalRole.ADMIN)
                .phone("01000000000")
                .build();
            entityManager.persist(professor);

            CourseJpaEntity course = CourseJpaEntity.builder()
                .name("소프트웨어공학")
                .year(2026)
                .semester(SemesterType.SPRING)
                .status(StatusType.ACTIVE)
                .build();
            entityManager.persist(course);

            SectionJpaEntity section = SectionJpaEntity.builder()
                .professor(professor)
                .course(course)
                .code("A")
                .name("A분반")
                .classTime("월 1-3")
                .capacity(40)
                .build();
            entityManager.persist(section);

            TeamJpaEntity team = TeamJpaEntity.builder()
                .section(section)
                .name("1팀")
                .kickoffRule("규칙")
                .meetingSchedule("매주 월요일")
                .status(Status.FORMING)
                .build();
            entityManager.persist(team);

            entityManager.flush();
            teamId = team.getId();
        });
    }

    @Test
    @DisplayName("동일 사용자가 서로 다른 주제 후보를 동시 등록 시 하나만 성공하고 다른 하나는 DuplicateTopicCandidateException이 발생한다")
    void concurrentCreateTopicCandidateBySameUser_SavesOnlyOneAndRejectsOther() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<TopicCandidate> task1 = () -> {
            start.await();
            return topicCandidateCommandService.createTopicCandidate(teamId, PROPOSER_USER_ID, "주제 A", "설명 A");
        };
        Callable<TopicCandidate> task2 = () -> {
            start.await();
            return topicCandidateCommandService.createTopicCandidate(teamId, PROPOSER_USER_ID, "주제 B", "설명 B");
        };

        // when
        Future<TopicCandidate> future1 = executor.submit(task1);
        Future<TopicCandidate> future2 = executor.submit(task2);
        start.countDown();

        int successCount = 0;
        int duplicateExceptionCount = 0;

        List<Future<TopicCandidate>> futures = List.of(future1, future2);
        for (Future<TopicCandidate> future : futures) {
            try {
                TopicCandidate result = future.get(30, TimeUnit.SECONDS);
                if (result != null) {
                    successCount++;
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof DuplicateTopicCandidateException) {
                    duplicateExceptionCount++;
                } else {
                    throw e;
                }
            }
        }
        executor.shutdownNow();

        // then
        assertThat(successCount).isEqualTo(1);
        assertThat(duplicateExceptionCount).isEqualTo(1);
        assertThat(candidateCount(PROPOSER_USER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 사용자의 동시 주제 후보 등록은 각각 정상 저장된다")
    void concurrentCreateTopicCandidateByDifferentUsers_SavesBoth() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<TopicCandidate> task1 = () -> {
            start.await();
            return topicCandidateCommandService.createTopicCandidate(teamId, PROPOSER_USER_ID, "주제 A", "설명 A");
        };
        Callable<TopicCandidate> task2 = () -> {
            start.await();
            return topicCandidateCommandService.createTopicCandidate(teamId, OTHER_PROPOSER_USER_ID, "주제 B", "설명 B");
        };

        // when
        Future<TopicCandidate> future1 = executor.submit(task1);
        Future<TopicCandidate> future2 = executor.submit(task2);
        start.countDown();

        TopicCandidate result1 = future1.get(30, TimeUnit.SECONDS);
        TopicCandidate result2 = future2.get(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(candidateCount(PROPOSER_USER_ID)).isEqualTo(1);
        assertThat(candidateCount(OTHER_PROPOSER_USER_ID)).isEqualTo(1);
        assertThat(totalCandidateCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("후보를 삭제한 뒤에는 같은 사용자가 다른 제목으로 다시 등록할 수 있다")
    void createTopicCandidate_AllowsResubmit_AfterSoftDelete() {
        // given: 사용자가 기존 주제를 등록한 후 소프트 삭제
        TopicCandidate candidate = topicCandidateCommandService.createTopicCandidate(
            teamId, PROPOSER_USER_ID, "기존 주제", "기존 설명"
        );
        topicCandidateCommandService.deleteTopicCandidate(candidate.getId());

        // when: 삭제된 행은 부분 유니크 인덱스에서 빠지므로 자리를 점유하지 않는다
        TopicCandidate resubmitted = topicCandidateCommandService.createTopicCandidate(
            teamId, PROPOSER_USER_ID, "새 주제", "새 설명"
        );

        // then
        assertThat(resubmitted.getId()).isNotEqualTo(candidate.getId());
        assertThat(candidateCount(PROPOSER_USER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("삭제된 후보가 쓰던 제목은 다른 사용자가 다시 쓸 수 있다")
    void createTopicCandidate_AllowsReusingTitle_OfSoftDeletedCandidate() {
        TopicCandidate candidate = topicCandidateCommandService.createTopicCandidate(
            teamId, PROPOSER_USER_ID, "기존 주제", "기존 설명"
        );
        topicCandidateCommandService.deleteTopicCandidate(candidate.getId());

        TopicCandidate reused = topicCandidateCommandService.createTopicCandidate(
            teamId, OTHER_PROPOSER_USER_ID, "기존 주제", "다른 설명"
        );

        assertThat(reused.getId()).isNotEqualTo(candidate.getId());
        assertThat(totalCandidateCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("부분 유니크 인덱스가 활성 행만 막는다: 앱을 우회한 중복 INSERT 는 거절되고, 삭제 표시된 행은 허용된다")
    void partialUniqueIndex_RejectsActiveDuplicateOnly() {
        topicCandidateCommandService.createTopicCandidate(teamId, PROPOSER_USER_ID, "기존 주제", "기존 설명");

        // 활성 행 중복은 DB 가 막는다 (앱 레벨 검사를 우회한 직접 INSERT)
        assertThatThrownBy(() -> insertDirectly(PROPOSER_USER_ID, "다른 제목", null))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertDirectly(OTHER_PROPOSER_USER_ID, "기존 주제", null))
            .isInstanceOf(DataIntegrityViolationException.class);

        // 삭제 표시된 행은 같은 자리를 차지해도 된다
        insertDirectly(PROPOSER_USER_ID, "기존 주제", LocalDateTime.now());
        assertThat(candidateCount(PROPOSER_USER_ID)).isEqualTo(1);
    }

    private void insertDirectly(String proposerUserId, String title, LocalDateTime deletedAt) {
        jdbcTemplate.update(
            "INSERT INTO topic_candidate (team_id, proposer_user_id, title, description, created_at, updated_at, deleted_at)"
                + " VALUES (?, ?, ?, '설명', now(), now(), ?)",
            teamId, proposerUserId, title, deletedAt
        );
    }

    // 부분 유니크 인덱스는 엔티티로 표현할 수 없어 ddl-auto 가 만들어주지 않는다.
    // 운영과 같은 제약 아래에서 검증하려면 DDL 파일을 그대로 적용해야 한다(IF NOT EXISTS 라 반복 실행 안전).
    private void applyPartialUniqueIndexes() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            Path ddl = current.resolve("database").resolve("topic_candidate.sql");
            if (Files.exists(ddl)) {
                try {
                    jdbcTemplate.execute(Files.readString(ddl, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("database/topic_candidate.sql 을 찾지 못했다");
    }

    private Integer candidateCount(String proposerUserId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM topic_candidate WHERE team_id = ? AND proposer_user_id = ? AND deleted_at IS NULL",
            Integer.class, teamId, proposerUserId
        );
    }

    private Integer totalCandidateCount() {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM topic_candidate WHERE team_id = ? AND deleted_at IS NULL",
            Integer.class, teamId
        );
    }
}
