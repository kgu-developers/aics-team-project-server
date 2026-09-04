package topicvote.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
import kgu.developers.domain.topicCandidate.infrastructure.TopicCandidateJpaEntity;
import kgu.developers.domain.topicCandidate.infrastructure.TopicCandidateRepositoryImpl;
import kgu.developers.domain.topicVote.application.command.TopicVoteCommandService;
import kgu.developers.domain.topicVote.infrastructure.TopicVoteRepositoryImpl;
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
 * 첫 투표는 잠글 투표 행이 없어 애플리케이션 코드만으로는 직렬화되지 않는다.
 * 실제 PostgreSQL 에서 팀 행 잠금이 동시 투표를 직렬화하는지 확인한다.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TopicVoteConcurrencyTest {

    private static final String VOTER_USER_ID = "202412345";

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
        TopicVoteCommandService.class,
        TopicVoteRepositoryImpl.class,
        TopicCandidateRepositoryImpl.class,
        TeamRepositoryImpl.class,
        TeamMemberRepositoryImpl.class
    })
    static class TestConfig {
    }

    @Autowired
    private TopicVoteCommandService topicVoteCommandService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long teamId;
    private Long candidateId;
    private Long otherCandidateId;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM topic_vote");
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

            TopicCandidateJpaEntity candidate = TopicCandidateJpaEntity.builder()
                .teamId(team.getId())
                .proposerUserId(VOTER_USER_ID)
                .title("주제 A")
                .description("설명")
                .build();
            entityManager.persist(candidate);

            TopicCandidateJpaEntity otherCandidate = TopicCandidateJpaEntity.builder()
                .teamId(team.getId())
                .proposerUserId("202412346")
                .title("주제 B")
                .description("설명")
                .build();
            entityManager.persist(otherCandidate);

            entityManager.flush();
            teamId = team.getId();
            candidateId = candidate.getId();
            otherCandidateId = otherCandidate.getId();
        });
    }

    @Test
    @DisplayName("같은 투표자의 동시 첫 투표는 한 행만 만든다")
    void concurrentFirstVotesCreateSingleRow() throws Exception {
        runConcurrently(
            () -> topicVoteCommandService.vote(teamId, candidateId, VOTER_USER_ID).getId(),
            () -> topicVoteCommandService.vote(teamId, otherCandidateId, VOTER_USER_ID).getId()
        );

        assertThat(voteCount(VOTER_USER_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 투표자의 동시 첫 투표는 각각 저장된다")
    void concurrentFirstVotesOfDifferentVotersAreBothSaved() throws Exception {
        runConcurrently(
            () -> topicVoteCommandService.vote(teamId, candidateId, VOTER_USER_ID).getId(),
            () -> topicVoteCommandService.vote(teamId, candidateId, "202412346").getId()
        );

        assertThat(voteCount(VOTER_USER_ID)).isEqualTo(1);
        assertThat(voteCount("202412346")).isEqualTo(1);
    }

    private void runConcurrently(Callable<Long> first, Callable<Long> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Long>> results = List.of(first, second).stream()
                .map(task -> executor.submit(() -> {
                    start.await();
                    return task.call();
                }))
                .toList();

            start.countDown();
            for (Future<Long> result : results) {
                assertThat(result.get(30, TimeUnit.SECONDS)).isNotNull();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Integer voteCount(String voterUserId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM topic_vote WHERE team_id = ? AND voter_user_id = ?",
            Integer.class, teamId, voterUserId
        );
    }
}
