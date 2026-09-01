package topicVote.infrastructure;

import kgu.developers.domain.topicVote.application.command.TopicVoteCommandService;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.exception.TopicVoteNotFoundException;
import kgu.developers.domain.topicVote.infrastructure.JpaTopicVoteRepository;
import kgu.developers.domain.topicVote.infrastructure.TopicVoteJpaEntity;
import kgu.developers.domain.topicVote.infrastructure.TopicVoteRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")  // 실 DB이므로 스키마 생성을 명시해야 한다
@AutoConfigureTestDatabase(replace = NONE)
@Import({TopicVoteRepositoryImpl.class, TopicVoteCommandService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 실제 커밋을 봐야 하므로 테스트 트랜잭션을 쓰지 않는다
@DirtiesContext
class TopicVoteRepositoryPostgresTest {

    /**
     * 기본은 Testcontainers(도커 필요). TEST_DB_URL 이 있으면 그 PostgreSQL 에 붙는다.
     * 주의: ddl-auto=create 이므로 반드시 전용 테스트 DB 를 가리켜야 한다.
     */
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DB_URL");
        if (url == null) {
            // docker/development/docker-compose.yml 과 동일 버전
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

    private static final Long TEAM = 1L;
    private static final Long OTHER_TEAM = 2L;
    private static final Long CANDIDATE_A = 10L;
    private static final Long CANDIDATE_B = 20L;
    private static final String VOTER = "20230001";

    @Autowired
    private TopicVoteCommandService topicVoteCommandService;

    @Autowired
    private TopicVoteRepositoryImpl topicVoteRepository;

    @Autowired
    private JpaTopicVoteRepository jpaTopicVoteRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @SpringBootApplication
    @EntityScan(basePackageClasses = TopicVoteJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = JpaTopicVoteRepository.class)
    static class TestApp {
    }

    @BeforeEach
    void clean() {
        jpaTopicVoteRepository.deleteAll();
    }

    private int activeVotesOf(String voterUserId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM topic_vote WHERE voter_user_id = ? AND deleted_at IS NULL",
                Integer.class, voterUserId);
    }

    @Test
    @DisplayName("전제 확인: 유니크 제약이 팀 기준이고 소프트 삭제된 행에도 적용된다")
    void uniqueConstraintIsTeamScopedAndAppliesToSoftDeletedRows() {
        String constraint = jdbc.queryForObject("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conrelid = 'topic_vote'::regclass AND contype = 'u'
                """, String.class);
        assertThat(constraint).isEqualTo("UNIQUE (team_id, voter_user_id)");

        topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);
        topicVoteCommandService.cancelVote(TEAM, VOTER);

        // 삭제된 행이 여전히 키를 점유하므로 맨 INSERT 는 반드시 실패한다 -> 재활성화가 필요한 이유
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO topic_vote (team_id, candidate_id, voter_user_id, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                """, TEAM, CANDIDATE_B, VOTER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("1인 1표: 같은 팀의 다른 후보에 투표하면 표가 늘지 않고 후보만 바뀐다")
    void oneVotePerPersonPerTeam() {
        TopicVote first = topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);

        TopicVote changed = topicVoteCommandService.vote(TEAM, CANDIDATE_B, VOTER);

        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
        assertThat(changed.getId()).isEqualTo(first.getId());   // 새 행이 아니라 같은 행
        assertThat(changed.getCandidateId()).isEqualTo(CANDIDATE_B);
        assertThat(topicVoteRepository.findAllByCandidateId(CANDIDATE_A)).isEmpty();
        assertThat(topicVoteRepository.findAllByCandidateId(CANDIDATE_B)).hasSize(1);
    }

    @Test
    @DisplayName("같은 후보에 다시 투표하면 멱등이다")
    void repeatedVoteForSameCandidateIsIdempotent() {
        TopicVote first = topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);

        TopicVote again = topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
    }

    @Test
    @DisplayName("투표 취소 후 다시 투표하면 삭제된 행이 재활성화된다 (다른 후보여도 된다)")
    void revoteAfterCancelReactivatesRow() {
        TopicVote first = topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);
        topicVoteCommandService.cancelVote(TEAM, VOTER);
        assertThat(topicVoteRepository.findByTeamIdAndVoterUserId(TEAM, VOTER)).isEmpty();

        TopicVote second = topicVoteCommandService.vote(TEAM, CANDIDATE_B, VOTER);

        assertThat(second.getId()).isEqualTo(first.getId());   // 새 INSERT 가 아니라 기존 행 재활성화
        assertThat(second.getDeletedAt()).isNull();
        assertThat(second.getCandidateId()).isEqualTo(CANDIDATE_B);
        assertThat(jpaTopicVoteRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("팀이 다르면 같은 사람도 각 팀에서 한 표씩 가진다")
    void voteIsScopedPerTeam() {
        topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);
        topicVoteCommandService.vote(OTHER_TEAM, CANDIDATE_B, VOTER);

        assertThat(activeVotesOf(VOTER)).isEqualTo(2);
        assertThat(topicVoteRepository.findByTeamIdAndVoterUserId(TEAM, VOTER)).isPresent();
        assertThat(topicVoteRepository.findByTeamIdAndVoterUserId(OTHER_TEAM, VOTER)).isPresent();
    }

    @Test
    @DisplayName("동시 최초 투표: 존재 확인이 check-then-act 이므로 진 쪽은 제약 위반이고, 활성 행은 하나만 남는다")
    void concurrentFirstVoteKeepsSingleActiveRow() throws Exception {
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<TopicVote>> tasks = java.util.Collections.nCopies(threads, () -> {
            barrier.await();
            return topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);
        });

        List<Future<TopicVote>> results = pool.invokeAll(tasks);
        pool.shutdown();

        int succeeded = 0;
        for (Future<TopicVote> result : results) {
            try {
                assertThat(result.get().getCandidateId()).isEqualTo(CANDIDATE_A);
                succeeded++;
            } catch (ExecutionException e) {
                // 중복 차단의 최종 근거는 유니크 제약이다. database/topic_vote.sql 참고.
                assertThat(e.getCause()).isInstanceOf(DataIntegrityViolationException.class);
            }
        }

        assertThat(succeeded).isGreaterThanOrEqualTo(1);
        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
        assertThat(jpaTopicVoteRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않거나 이미 취소된 투표를 취소하면 TopicVoteNotFoundException")
    void cancelMissingOrAlreadyDeletedVote() {
        assertThatThrownBy(() -> topicVoteCommandService.cancelVote(TEAM, VOTER))
                .isInstanceOf(TopicVoteNotFoundException.class);

        TopicVote vote = topicVoteCommandService.vote(TEAM, CANDIDATE_A, VOTER);
        topicVoteCommandService.cancelVote(TEAM, VOTER);

        assertThatThrownBy(() -> topicVoteCommandService.cancelVote(TEAM, VOTER))
                .isInstanceOf(TopicVoteNotFoundException.class);
        assertThatThrownBy(() -> topicVoteRepository.deleteById(vote.getId()))
                .isInstanceOf(TopicVoteNotFoundException.class);
    }
}
