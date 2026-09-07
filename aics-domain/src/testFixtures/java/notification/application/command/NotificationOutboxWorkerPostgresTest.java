package notification.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.time.LocalDateTime;
import kgu.developers.domain.notification.application.command.NotificationOutboxWorker;
import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import kgu.developers.domain.notification.domain.NotificationRepository;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.notification.domain.OutboxStatus;
import kgu.developers.domain.notification.infrastructure.JpaNotificationOutboxRepository;
import kgu.developers.domain.notification.infrastructure.JpaNotificationRepository;
import kgu.developers.domain.notification.infrastructure.NotificationJpaEntity;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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

/**
 * 아웃박스 처리의 트랜잭션 경계를 실 DB 로 검증한다. 성공·실패가 각각 독립 트랜잭션이라
 * 한 건이 터져도 실패 표시(retryCount)가 남고 다른 건은 영향을 받지 않아야 한다.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")  // 실 DB이므로 스키마 생성을 명시해야 한다
@AutoConfigureTestDatabase(replace = NONE)
@Import({NotificationOutboxRepositoryImpl.class, NotificationOutboxWorkerPostgresTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 워커가 자기 트랜잭션을 열어야 하므로 테스트 트랜잭션을 쓰지 않는다
@DirtiesContext
class NotificationOutboxWorkerPostgresTest {

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
    private static final String FAILING_USER = "99999999";   // 이 학번의 알림 저장은 항상 터진다

    @SpringBootApplication
    @EntityScan(basePackageClasses = NotificationOutboxJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = JpaNotificationOutboxRepository.class)
    static class TestApp {
    }

    @TestConfiguration
    static class Config {

        /** 알림 저장 실패를 재현하려고 특정 학번만 예외를 던지는 것 외에는 진짜 저장과 같다. */
        @Bean
        NotificationRepository notificationRepository(JpaNotificationRepository jpaNotificationRepository) {
            return notification -> {
                if (FAILING_USER.equals(notification.getUserId())) {
                    throw new IllegalStateException("알림 저장 실패");
                }
                return jpaNotificationRepository.save(NotificationJpaEntity.toEntity(notification)).toDomain();
            };
        }

        @Bean
        NotificationOutboxWorker notificationOutboxWorker(
            NotificationOutboxRepository notificationOutboxRepository,
            NotificationRepository notificationRepository
        ) {
            return new NotificationOutboxWorker(notificationOutboxRepository, notificationRepository);
        }
    }

    @Autowired
    private NotificationOutboxWorker worker;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private JpaNotificationOutboxRepository jpaOutboxRepository;

    @Autowired
    private JpaNotificationRepository jpaNotificationRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        jpaOutboxRepository.deleteAll();
        jpaNotificationRepository.deleteAll();
    }

    private Long saveOutbox(String userId) {
        return saveOutbox(userId, NotificationType.SECTION_ANNOUNCEMENT);
    }

    private Long saveOutbox(String userId, NotificationType type) {
        return outboxRepository.save(NotificationOutbox.builder()
            .userId(userId)
            .type(type)
            .sourceId(1L)
            .title("조원 지목 요청")
            .message("김철수 님이 회원님을 조원으로 지목했습니다.")
            .link("/link")
            .status(OutboxStatus.PENDING)
            .createdAt(LocalDateTime.now().minusMinutes(1))
            .retryCount(0)
            .build()).getId();
    }

    private NotificationOutbox reload(Long id) {
        return new TransactionTemplate(transactionManager)
            .execute(status -> outboxRepository.lockById(id).orElse(null));
    }

    @Test
    @DisplayName("처리에 성공하면 알림이 생기고 아웃박스 항목은 사라진다")
    void processCreatesNotificationAndRemovesOutbox() {
        Long id = saveOutbox("20230001");

        worker.process(id);

        assertThat(jpaOutboxRepository.findById(id)).isEmpty();
        assertThat(jpaNotificationRepository.findAll())
            .singleElement()
            .satisfies(saved -> {
                assertThat(saved.getUserId()).isEqualTo("20230001");
                assertThat(saved.getTitle()).isEqualTo("조원 지목 요청");
                assertThat(saved.isRead()).isFalse();
            });
    }

    @Test
    @DisplayName("모든 알림 타입이 아웃박스에 저장되고 알림까지 처리된다")
    void everyTypeIsPersistedAndProcessed() {
        for (NotificationType type : NotificationType.values()) {
            worker.process(saveOutbox("20230001", type));
        }

        assertThat(jpaOutboxRepository.findAll()).isEmpty();
        assertThat(jpaNotificationRepository.findAll())
            .extracting(NotificationJpaEntity::getType)
            .containsExactlyInAnyOrder(NotificationType.values());
    }

    @Test
    @DisplayName("알림 저장이 터지면 아웃박스는 롤백으로 살아남고, 실패 표시는 별도 트랜잭션에 남는다")
    void failedProcessRollsBackButRecordsRetry() {
        Long id = saveOutbox(FAILING_USER);

        assertThatThrownBy(() -> worker.process(id)).isInstanceOf(IllegalStateException.class);

        // 처리 트랜잭션은 통째로 롤백됐다 — 알림도 없고 아웃박스도 그대로다
        assertThat(jpaNotificationRepository.findAll()).isEmpty();
        assertThat(jpaOutboxRepository.findById(id)).isPresent();

        worker.markFailed(id, MAX_RETRIES, "알림 저장 실패");

        NotificationOutbox failed = reload(id);
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);   // 여기가 0 이면 영원히 같은 건을 재시도한다
        assertThat(failed.getErrorMessage()).isEqualTo("알림 저장 실패");
    }

    @Test
    @DisplayName("재시도를 반복하면 최대 횟수에서 대상 목록에서 빠진다")
    void retriesStopAtMaxRetries() {
        Long id = saveOutbox(FAILING_USER);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            assertThatThrownBy(() -> worker.process(id)).isInstanceOf(IllegalStateException.class);
            worker.markFailed(id, MAX_RETRIES, "알림 저장 실패");

            assertThat(reload(id).getRetryCount()).isEqualTo(attempt);
        }

        assertThat(dueIds()).doesNotContain(id);
        assertThat(jpaOutboxRepository.findById(id)).isPresent();   // 사라지진 않는다 — 사람이 볼 수 있게 남는다
    }

    @Test
    @DisplayName("한 건이 실패해도 같은 배치의 다른 건은 그대로 처리된다")
    void failureDoesNotAffectOtherEntries() {
        Long failing = saveOutbox(FAILING_USER);
        Long healthy = saveOutbox("20230001");

        assertThatThrownBy(() -> worker.process(failing)).isInstanceOf(IllegalStateException.class);
        worker.markFailed(failing, MAX_RETRIES, "알림 저장 실패");
        worker.process(healthy);

        assertThat(jpaOutboxRepository.findById(healthy)).isEmpty();
        assertThat(jpaNotificationRepository.findAll()).singleElement()
            .satisfies(saved -> assertThat(saved.getUserId()).isEqualTo("20230001"));
        assertThat(reload(failing).getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("다른 인스턴스가 이미 처리해 행이 사라졌으면 조용히 넘어간다")
    void processIsNoOpWhenRowIsGone() {
        Long id = saveOutbox("20230001");
        jpaOutboxRepository.deleteById(id);

        worker.process(id);
        worker.markFailed(id, MAX_RETRIES, "알림 저장 실패");

        assertThat(jpaNotificationRepository.findAll()).isEmpty();
    }

    private java.util.List<Long> dueIds() {
        return outboxRepository.findDueOutboxIds(MAX_RETRIES, LocalDateTime.now(), 50);
    }
}
