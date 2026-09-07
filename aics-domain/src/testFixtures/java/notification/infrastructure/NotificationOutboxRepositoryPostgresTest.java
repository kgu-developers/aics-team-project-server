package notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.notification.domain.OutboxStatus;
import kgu.developers.domain.notification.infrastructure.JpaNotificationOutboxRepository;
import kgu.developers.domain.notification.infrastructure.NotificationOutboxJpaEntity;
import kgu.developers.domain.notification.infrastructure.NotificationOutboxRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")  // 실 DB이므로 스키마 생성을 명시해야 한다
@AutoConfigureTestDatabase(replace = NONE)
@Import(NotificationOutboxRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 잠금 경합과 커밋을 봐야 하므로 테스트 트랜잭션을 쓰지 않는다
@DirtiesContext
class NotificationOutboxRepositoryPostgresTest {

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

    private static final int MAX_RETRIES = 3;
    private static final int FIRST_ATTEMPT_DELAY_SECONDS = 10;   // NotificationOutbox.FIRST_ATTEMPT_DELAY
    private static final int RETRY_BASE_INTERVAL_SECONDS = 30;   // NotificationOutbox.RETRY_BASE_INTERVAL

    @Autowired
    private NotificationOutboxRepositoryImpl outboxRepository;

    @Autowired
    private JpaNotificationOutboxRepository jpaOutboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @SpringBootApplication
    @EntityScan(basePackageClasses = NotificationOutboxJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = JpaNotificationOutboxRepository.class)
    static class TestApp {
    }

    @BeforeEach
    void clean() {
        jpaOutboxRepository.deleteAll();
    }

    private Long saveOutbox(String userId, LocalDateTime createdAt) {
        return outboxRepository.save(NotificationOutbox.builder()
            .userId(userId)
            .type(NotificationType.SECTION_ANNOUNCEMENT)
            .sourceId(1L)
            .title("제목")
            .message("내용")
            .link("/link")
            .status(OutboxStatus.PENDING)
            .createdAt(createdAt)
            .nextAttemptAt(createdAt.plusSeconds(FIRST_ATTEMPT_DELAY_SECONDS))
            .retryCount(0)
            .build()).getId();
    }

    private List<Long> dueIds() {
        return dueIds(LocalDateTime.now());
    }

    /** 간격이 지난 상황은 "지금"을 앞당겨 만든다 — 실제로 기다리지 않는다. */
    private List<Long> dueIds(LocalDateTime now) {
        return outboxRepository.findDueOutboxIds(MAX_RETRIES, now, 50);
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("10초 지연: 방금 만들어진 아웃박스는 아직 집히지 않는다")
    void freshOutboxIsNotDueYet() {
        saveOutbox("20230001", LocalDateTime.now());

        assertThat(dueIds()).isEmpty();
    }

    @Test
    @DisplayName("다중 인스턴스: 같은 행을 동시에 잡으면 한쪽만 가져가고 다른 쪽은 건너뛴다")
    void concurrentLockOnSameRowSucceedsOnlyOnce() throws Exception {
        Long id = saveOutbox("20230001", LocalDateTime.now().minusMinutes(1));
        assertThat(dueIds()).containsExactly(id);

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<Boolean>> results = pool.invokeAll(List.of(
            () -> lockInOpenTransaction(id, barrier),
            () -> lockInOpenTransaction(id, barrier)));
        pool.shutdown();

        // SKIP LOCKED 가 없으면 뒤쪽이 잠금 해제를 기다리다 배리어에서 같이 멈춘다
        assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(true, false);
    }

    private boolean lockInOpenTransaction(Long id, CyclicBarrier barrier) {
        return Boolean.TRUE.equals(tx().execute(status -> {
            boolean acquired = outboxRepository.lockById(id).isPresent();
            try {
                barrier.await(5, TimeUnit.SECONDS);   // 상대가 시도할 때까지 잠금을 쥐고 있는다 (막히면 걸린다)
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return acquired;
        }));
    }

    @Test
    @DisplayName("처리 트랜잭션이 롤백돼도 별도 트랜잭션의 실패 표시는 살아남는다")
    void failureIsRecordedInSeparateTransaction() {
        Long id = saveOutbox("20230001", LocalDateTime.now().minusMinutes(1));

        // 처리 트랜잭션이 터져서 롤백되는 상황
        try {
            tx().executeWithoutResult(status -> {
                outboxRepository.lockById(id).orElseThrow();
                throw new IllegalStateException("알림 저장 실패");
            });
        } catch (IllegalStateException ignored) {
        }

        // 롤백 이후 새 트랜잭션에서 실패를 남긴다
        tx().executeWithoutResult(status -> {
            NotificationOutbox outbox = outboxRepository.lockById(id).orElseThrow();
            outbox.markAsFailed("알림 저장 실패");
            outboxRepository.save(outbox);
        });

        Optional<NotificationOutbox> reloaded = tx().execute(status -> outboxRepository.lockById(id));
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(reloaded.get().getRetryCount()).isEqualTo(1);   // 여기가 0 이면 영원히 재시도한다
        assertThat(reloaded.get().getNextAttemptAt()).isAfter(LocalDateTime.now());   // 백오프가 걸려 있어야 한다
    }

    @Test
    @DisplayName("실패한 아웃박스는 재시도 간격이 지나기 전까지 다시 집히지 않는다")
    void failedOutboxWaitsForRetryInterval() {
        Long id = saveOutbox("20230001", LocalDateTime.now().minusMinutes(1));

        tx().executeWithoutResult(status -> {
            NotificationOutbox outbox = outboxRepository.lockById(id).orElseThrow();
            outbox.markAsFailed("알림 저장 실패");
            outboxRepository.save(outbox);
        });

        // 이 단언이 깨지면 5초짜리 스케줄이 재시도 횟수를 순식간에 다 태운다
        assertThat(dueIds()).isEmpty();
        assertThat(dueIds(LocalDateTime.now().plusSeconds(RETRY_BASE_INTERVAL_SECONDS + 1))).containsExactly(id);
    }

    @Test
    @DisplayName("실패가 쌓이면 재시도 간격이 두 배씩 벌어진다")
    void retryIntervalDoublesOnEachFailure() {
        Long id = saveOutbox("20230001", LocalDateTime.now().minusMinutes(1));

        markFailed(id);
        LocalDateTime afterFirst = LocalDateTime.now().plusSeconds(RETRY_BASE_INTERVAL_SECONDS + 1);
        assertThat(dueIds(afterFirst)).containsExactly(id);

        markFailed(id);
        assertThat(dueIds(afterFirst)).isEmpty();   // 두 번째 실패 뒤엔 30초로는 부족하다
        assertThat(dueIds(LocalDateTime.now().plusSeconds(2 * RETRY_BASE_INTERVAL_SECONDS + 1)))
            .containsExactly(id);
    }

    private void markFailed(Long id) {
        tx().executeWithoutResult(status -> {
            NotificationOutbox outbox = outboxRepository.lockById(id).orElseThrow();
            outbox.markAsFailed("알림 저장 실패");
            outboxRepository.save(outbox);
        });
    }

    @Test
    @DisplayName("최대 재시도를 넘긴 아웃박스는 더 이상 집히지 않는다")
    void exhaustedOutboxIsNotDue() {
        Long id = saveOutbox("20230001", LocalDateTime.now().minusMinutes(1));

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            tx().executeWithoutResult(status -> {
                NotificationOutbox outbox = outboxRepository.lockById(id).orElseThrow();
                outbox.markAsFailed("실패");
                outboxRepository.save(outbox);
            });
        }

        // 간격이 아무리 지나도 횟수를 다 썼으면 안 잡힌다
        assertThat(dueIds(LocalDateTime.now().plusDays(1))).isEmpty();
    }
}
