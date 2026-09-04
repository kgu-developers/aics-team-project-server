package topicVote.infrastructure;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")  // 실 DB이므로 스키마 생성을 명시해야 한다
@AutoConfigureTestDatabase(replace = NONE)
@Import(TopicVoteRepositoryImpl.class)
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
    private TopicVoteRepositoryImpl topicVoteRepository;

    @Autowired
    private JpaTopicVoteRepository jpaTopicVoteRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    private long versionOf(String voterUserId) {
        return jdbc.queryForObject(
                "SELECT version FROM topic_vote WHERE voter_user_id = ?", Long.class, voterUserId);
    }

    @Test
    @DisplayName("전제 확인: 유니크 제약이 팀 기준이고 소프트 삭제된 행에도 적용된다")
    void uniqueConstraintIsTeamScopedAndAppliesToSoftDeletedRows() {
        String constraint = jdbc.queryForObject("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint
                WHERE conrelid = 'topic_vote'::regclass AND contype = 'u'
                """, String.class);
        assertThat(constraint).isEqualTo("UNIQUE (team_id, voter_user_id)");

        topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));
        topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER);

        // 삭제된 행이 여전히 키를 점유하므로 맨 INSERT 는 반드시 실패한다 -> 재활성화가 필요한 이유
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO topic_vote (team_id, candidate_id, voter_user_id, version, created_at, updated_at)
                VALUES (?, ?, ?, 0, now(), now())
                """, TEAM, CANDIDATE_B, VOTER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("1인 1표: 같은 팀의 다른 후보에 투표하면 표가 늘지 않고 후보만 바뀐다")
    void oneVotePerPersonPerTeam() {
        TopicVote first = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));

        TopicVote changed = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_B, VOTER));

        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
        assertThat(changed.getId()).isEqualTo(first.getId());   // 새 행이 아니라 같은 행
        assertThat(changed.getCandidateId()).isEqualTo(CANDIDATE_B);
        assertThat(topicVoteRepository.findAllByCandidateId(CANDIDATE_A)).isEmpty();
        assertThat(topicVoteRepository.findAllByCandidateId(CANDIDATE_B)).hasSize(1);
    }

    @Test
    @DisplayName("같은 후보에 다시 투표하면 멱등이다")
    void repeatedVoteForSameCandidateIsIdempotent() {
        TopicVote first = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));

        TopicVote again = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
        assertThat(versionOf(VOTER)).isEqualTo(first.getVersion() + 1);   // 업서트가 버전을 올려야 낙관적 락이 산다
    }

    @Test
    @DisplayName("투표 취소 후 다시 투표하면 삭제된 행이 재활성화된다 (다른 후보여도 된다)")
    void revoteAfterCancelReactivatesRow() {
        TopicVote first = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));
        topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER);
        assertThat(topicVoteRepository.findByTeamIdAndVoterUserId(TEAM, VOTER)).isEmpty();

        TopicVote second = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_B, VOTER));

        assertThat(second.getId()).isEqualTo(first.getId());   // 새 INSERT 가 아니라 기존 행 재활성화
        assertThat(second.getDeletedAt()).isNull();
        assertThat(second.getCandidateId()).isEqualTo(CANDIDATE_B);
        assertThat(jpaTopicVoteRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("팀이 다르면 같은 사람도 각 팀에서 한 표씩 가진다")
    void voteIsScopedPerTeam() {
        topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));
        topicVoteRepository.upsert(TopicVote.create(OTHER_TEAM, CANDIDATE_B, VOTER));

        assertThat(activeVotesOf(VOTER)).isEqualTo(2);
        assertThat(topicVoteRepository.findByTeamIdAndVoterUserId(TEAM, VOTER)).isPresent();
        assertThat(topicVoteRepository.findByTeamIdAndVoterUserId(OTHER_TEAM, VOTER)).isPresent();
    }

    @Test
    @DisplayName("동시 최초 투표: 업서트라 모두 성공하고, 활성 행은 하나만 남는다")
    void concurrentFirstVoteKeepsSingleActiveRow() throws Exception {
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<TopicVote>> tasks = java.util.Collections.nCopies(threads, () -> {
            barrier.await();
            return topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));
        });

        List<Future<TopicVote>> results = pool.invokeAll(tasks);
        pool.shutdown();

        for (Future<TopicVote> result : results) {
            assertThat(result.get().getCandidateId()).isEqualTo(CANDIDATE_A);
            assertThat(result.get().getId()).isEqualTo(results.get(0).get().getId());
        }

        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
        assertThat(jpaTopicVoteRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않거나 이미 취소된 투표를 취소하면 TopicVoteNotFoundException")
    void cancelMissingOrAlreadyDeletedVote() {
        assertThatThrownBy(() -> topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER))
                .isInstanceOf(TopicVoteNotFoundException.class);

        TopicVote vote = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));
        topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER);

        assertThatThrownBy(() -> topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER))
                .isInstanceOf(TopicVoteNotFoundException.class);
        assertThatThrownBy(() -> topicVoteRepository.deleteById(vote.getId()))
                .isInstanceOf(TopicVoteNotFoundException.class);
    }

    @Test
    @DisplayName("후보가 다르면 취소되지 않고 기존 표가 유지된다")
    void cancelWithMismatchedCandidateKeepsVote() {
        topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));

        assertThatThrownBy(() -> topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_B, VOTER))
                .isInstanceOf(TopicVoteNotFoundException.class);

        assertThat(activeVotesOf(VOTER)).isEqualTo(1);
        assertThat(topicVoteRepository.findAllByCandidateId(CANDIDATE_A)).hasSize(1);
    }

    @Test
    @DisplayName("조회 후 다른 요청이 행을 바꾸면 취소는 낙관적 락에 걸려 TopicVoteNotFoundException")
    void staleCancelFailsWithNotFound() throws Exception {
        TopicVote vote = topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // 영속성 컨텍스트에 버전 0 인 상태로 올려둔다
            jpaTopicVoteRepository.findByTeamIdAndVoterUserIdAndDeletedAtIsNull(TEAM, VOTER).orElseThrow();

            bumpVersionInAnotherTransaction(vote.getId());

            // 취소는 캐시된 버전 0 으로 UPDATE 를 날리므로 반드시 진다
            topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER);
        })).isInstanceOf(TopicVoteNotFoundException.class);

        assertThat(activeVotesOf(VOTER)).isEqualTo(1);   // 롤백됐으니 표는 그대로다
    }

    /** 같은 스레드에서 JdbcTemplate 을 쓰면 진행 중인 트랜잭션에 붙어버리므로 별도 스레드에서 커밋시킨다. */
    private void bumpVersionInAnotherTransaction(Long id) {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> jdbc.update("UPDATE topic_vote SET version = version + 1 WHERE id = ?", id)).get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("동시 취소: 낙관적 락으로 한 번만 성공하고 나머지는 TopicVoteNotFoundException")
    void concurrentCancelSucceedsOnlyOnce() throws Exception {
        topicVoteRepository.upsert(TopicVote.create(TEAM, CANDIDATE_A, VOTER));

        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<Boolean>> tasks = java.util.Collections.nCopies(threads, () -> {
            barrier.await();
            try {
                topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(TEAM, CANDIDATE_A, VOTER);
                return true;
            } catch (TopicVoteNotFoundException e) {
                return false;
            }
        });

        List<Future<Boolean>> results = pool.invokeAll(tasks);
        pool.shutdown();

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(activeVotesOf(VOTER)).isZero();
    }
}
