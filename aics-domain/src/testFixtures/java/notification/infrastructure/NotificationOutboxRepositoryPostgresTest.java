package notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 잠금 경합을 봐야 하므로 테스트 트랜잭션을 쓰지 않는다
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

    private void saveOutbox(String userId, LocalDateTime createdAt) {
        outboxRepository.save(NotificationOutbox.builder()
            .userId(userId)
            .type(NotificationType.SECTION_ANNOUNCEMENT)
            .sourceId(1L)
            .title("제목")
            .message("내용")
            .link("/link")
            .status(OutboxStatus.PENDING)
            .createdAt(createdAt)
            .retryCount(0)
            .build());
    }

    /** 비관적 잠금이라 트랜잭션 안에서만 부를 수 있다. */
    private List<NotificationOutbox> lockBatch(int limit) {
        return outboxRepository.lockPendingOrRetryableOutboxesBefore(
            MAX_RETRIES, LocalDateTime.now().minusSeconds(10), limit);
    }

    @Test
    @DisplayName("10초 지연: 방금 만들어진 아웃박스는 아직 집히지 않는다")
    void freshOutboxIsNotPickedUpYet() {
        saveOutbox("20230001", LocalDateTime.now());

        List<NotificationOutbox> locked = new TransactionTemplate(transactionManager)
            .execute(status -> lockBatch(50));

        assertThat(locked).isEmpty();
    }

    @Test
    @DisplayName("다중 인스턴스: 동시에 잡아도 같은 아웃박스를 두 번 집지 않는다")
    void concurrentBatchesDoNotOverlap() throws Exception {
        LocalDateTime old = LocalDateTime.now().minusMinutes(1);
        for (int i = 0; i < 6; i++) {
            saveOutbox("2023000" + i, old);
        }

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // 두 인스턴스가 각각 3건씩 잡되, 트랜잭션을 열어둔 채로 상대를 기다린다
        List<Future<List<Long>>> results = pool.invokeAll(List.of(
            () -> lockInOpenTransaction(barrier),
            () -> lockInOpenTransaction(barrier)));
        pool.shutdown();

        List<Long> first = results.get(0).get();
        List<Long> second = results.get(1).get();

        assertThat(first).hasSize(3);
        assertThat(second).hasSize(3);
        assertThat(first).doesNotContainAnyElementsOf(second);   // SKIP LOCKED 가 없으면 여기서 겹친다
    }

    private List<Long> lockInOpenTransaction(CyclicBarrier barrier) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            List<Long> ids = lockBatch(3).stream().map(NotificationOutbox::getId).toList();
            try {
                barrier.await();   // 상대가 잡을 때까지 잠금을 쥐고 있는다
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return ids;
        });
    }
}
