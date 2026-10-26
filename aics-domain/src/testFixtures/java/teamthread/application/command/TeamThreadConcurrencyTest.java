package teamthread.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.infrastructure.TeamThreadRepositoryImpl;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 같은 팀의 TeamThread를 두 요청이 동시에 처음 생성하려 할 때(KD3-302) 500 없이
 * 둘 다 같은 스레드로 수렴하는지를 실제 PostgreSQL 환경(Testcontainers)에서 검증한다.
 *
 * 실제 운영 경로(TeamMessageFacade)는 이미 열려있는 @Transactional 트랜잭션 안에서
 * getOrCreateThread()를 호출한다 — 원래 버그(self-invocation으로 REQUIRES_NEW가 안 먹혀
 * insert 실패 시 Postgres가 그 바깥 트랜잭션 전체를 abort시키는 것)는 그 바깥 트랜잭션이
 * 열려있을 때만 재현된다. 서비스를 트랜잭션 없이 맨몸으로 직접 호출하면 Spring Data JPA가
 * 각 리포지토리 호출마다 자체 트랜잭션을 열어버려 이 조건이 재현되지 않으므로,
 * TransactionalCaller로 그 바깥 트랜잭션 경계를 재현한다.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TeamThreadConcurrencyTest {

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
    @org.springframework.context.annotation.Import({
        TeamThreadCommandService.class,
        TeamThreadRepositoryImpl.class,
        TeamThreadConcurrencyTest.TransactionalCaller.class
    })
    static class TestConfig {
    }

    // TeamMessageFacade(@Transactional)가 getOrCreateThread()를 호출하는 바깥 트랜잭션 경계를
    // 흉내만 낸다 — 전체 facade(의존성 다수)를 그대로 조립하지 않고도 버그 재현 조건을 맞춘다.
    @org.springframework.stereotype.Component
    @org.springframework.transaction.annotation.Transactional
    static class TransactionalCaller {
        private final TeamThreadCommandService teamThreadCommandService;

        TransactionalCaller(TeamThreadCommandService teamThreadCommandService) {
            this.teamThreadCommandService = teamThreadCommandService;
        }

        TeamThread getOrCreateThread(Long teamId) {
            return teamThreadCommandService.getOrCreateThread(teamId);
        }
    }

    @Autowired
    private TransactionalCaller transactionalCaller;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long teamId;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM team_thread");
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
    @DisplayName("메시지 등록 흐름(바깥 트랜잭션 안)에서 두 요청이 동시에 처음 생성하려 하면 예외 없이 같은 스레드로 수렴한다")
    void concurrentGetOrCreateThread_SameTeam_ConvergesToSameThreadWithoutException() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<TeamThread> task = () -> {
            start.await();
            return transactionalCaller.getOrCreateThread(teamId);
        };

        // when
        Future<TeamThread> future1 = executor.submit(task);
        Future<TeamThread> future2 = executor.submit(task);
        start.countDown();

        TeamThread result1 = future1.get(30, TimeUnit.SECONDS);
        TeamThread result2 = future2.get(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then — 둘 다 예외 없이 응답하고, 같은 스레드 id로 수렴하며, 실제 행은 하나만 남는다
        List<Long> threadIds = List.of(result1.getId(), result2.getId()).stream().distinct().collect(Collectors.toList());
        assertThat(threadIds).hasSize(1);
        assertThat(threadCount(teamId)).isEqualTo(1);
    }

    private Integer threadCount(Long teamId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM team_thread WHERE team_id = ?", Integer.class, teamId
        );
    }
}
