package preSurveyResponse.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.enrollment.infrastructure.EnrollmentRepositoryImpl;
import kgu.developers.domain.enrollment.infrastructure.JpaEnrollmentRepository;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
import kgu.developers.domain.preSurveyResponse.infrastructure.JpaPreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.infrastructure.PreSurveyResponseRepositoryImpl;

/**
 * 실제 DB(H2, PostgreSQL 모드)에 두 트랜잭션을 동시에 태워, 지목한 쪽의 재제출과 지목당한 쪽의
 * 수락이 서로의 쓰기를 덮어쓰지 않는지 검증한다. 두 경로 모두 지목한 쪽 Enrollment 행을
 * 비관적으로 잠그므로 직렬화돼야 한다 — 잠금을 빼면 이 테스트가 깨진다.
 */
@SpringBootTest(
    classes = PreSurveyResponseConcurrencyTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:pre_survey_response;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
    })
class PreSurveyResponseConcurrencyTest {

    private static final Long SECTION_ID = 1L;
    private static final String REQUESTER = "202412345";
    private static final String PEER = "202454321";

    @Autowired
    private PreSurveyResponseCommandService commandService;

    @Autowired
    private JpaPreSurveyResponseRepository jpaPreSurveyResponseRepository;

    @Autowired
    private JpaEnrollmentRepository jpaEnrollmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // 경합을 한 번만 돌리면 두 스레드가 우연히 겹치지 않아 락이 없어도 통과할 수 있다. 여러 번 돌려
    // 잃어버린 갱신이 드러날 확률을 올린다. 락을 빼면 이 횟수 안에서 반드시 깨지는 걸 확인했다.
    private static final int RACE_ROUNDS = 20;

    @Autowired
    private mock.repository.FakeUserQueryService fakeUserQueryService;

    @BeforeEach
    void setUp() {
        jpaPreSurveyResponseRepository.deleteAll();
        jpaEnrollmentRepository.deleteAll();
        enrollmentRepository.save(Enrollment.create(SECTION_ID, REQUESTER, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER, Role.STUDENT, Status.ACTIVE));
        fakeUserQueryService.save(kgu.developers.domain.user.domain.User.create(REQUESTER, "requester@test.com", "Requester", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
        fakeUserQueryService.save(kgu.developers.domain.user.domain.User.create(PEER, "peer@test.com", "Peer", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
    }

    @AfterEach
    void tearDown() {
        jpaPreSurveyResponseRepository.deleteAll();
        jpaEnrollmentRepository.deleteAll();
        fakeUserQueryService.clear();
    }

    /**
     * 두 순서 모두 (ACCEPTED, "바꾼 주제") 로 수렴하므로 이 단언은 실행 순서를 가정하지 않는다.
     * <ul>
     *   <li>재제출 → 수락 : 재제출이 PENDING 을 유지하고, 수락이 ACCEPTED 로 바꾼다</li>
     *   <li>수락 → 재제출 : 수락이 ACCEPTED 로 바꾸고, 재제출은 대상이 그대로라 상태를 건드리지 않는다</li>
     * </ul>
     * "대상이 같으면 상태를 보존한다"는 규칙 자체는 PreSurveyResponseTest 의
     * updateKeepsDecisionWhenPeerUnchanged 가 스레드 없이 고정한다. 그 규칙을 바꾸면 그쪽이 먼저
     * 깨지므로, 여기가 깨졌다면 규칙이 아니라 잠금이 무너진 것으로 읽으면 된다.
     */
    @Test
    @DisplayName("지목한 쪽의 재제출과 지목당한 쪽의 수락이 동시에 일어나도 서로의 쓰기를 잃지 않는다")
    void resubmitAndAcceptConcurrently() throws Exception {
        for (int round = 1; round <= RACE_ROUNDS; round++) {
            jpaPreSurveyResponseRepository.deleteAll();
            commandService.submit(REQUESTER, SECTION_ID, JsonConverter.parse("[\"BACKEND\"]"), "처음 주제", null, PEER);

            List<Throwable> failures = runConcurrently(
                () -> commandService.submit(
                    REQUESTER, SECTION_ID, JsonConverter.parse("[\"FRONTEND\"]"), "바꾼 주제", null, PEER),
                () -> commandService.decidePreferredPeer(PEER, SECTION_ID, REQUESTER, true));

            assertThat(failures).as("%d 번째 경합에서 예외가 났다", round).isEmpty();

            PreSurveyResponse saved = jpaPreSurveyResponseRepository
                    .findFirstByUserIdAndSectionIdAndDeletedAtIsNullOrderByIdDesc(REQUESTER, SECTION_ID)
                    .orElseThrow()
                    .toDomain();
            assertThat(saved.getPreferredPeerStatus())
                    .as("%d 번째 경합에서 수락이 재제출에 덮여 사라졌다", round)
                    .isEqualTo(PreferredPeerStatus.ACCEPTED);
            assertThat(saved.getTopicOpinion())
                    .as("%d 번째 경합에서 재제출이 수락에 덮여 사라졌다", round)
                    .isEqualTo("바꾼 주제");
            assertThat(saved.getPreferredPeerUserId()).isEqualTo(PEER);
        }
    }

    private List<Throwable> runConcurrently(Runnable... tasks) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks.length);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
        try {
            for (Runnable task : tasks) {
                executor.submit(() -> {
                    try {
                        start.await();
                        task.run();
                    } catch (Throwable e) {
                        failures.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
        return failures;
    }

    @Configuration
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class
    })
    @EntityScan("kgu.developers")
    @EnableJpaRepositories(basePackageClasses = {JpaPreSurveyResponseRepository.class, JpaEnrollmentRepository.class})
    @Import({PreSurveyResponseRepositoryImpl.class, EnrollmentRepositoryImpl.class})
    static class TestConfig {

        @Bean
        PreSurveyResponseCommandService preSurveyResponseCommandService(
            PreSurveyResponseRepository preSurveyResponseRepository,
            EnrollmentRepository enrollmentRepository,
            kgu.developers.domain.notification.application.command.NotificationOutboxService notificationOutboxService,
            kgu.developers.domain.user.application.query.UserQueryService userQueryService) {
            return new PreSurveyResponseCommandService(preSurveyResponseRepository, enrollmentRepository, notificationOutboxService, userQueryService);
        }

        @Bean
        kgu.developers.domain.notification.application.command.NotificationOutboxService notificationOutboxService() {
            return new mock.repository.FakeNotificationOutboxService();
        }

        @Bean
        kgu.developers.domain.user.application.query.UserQueryService userQueryService() {
            return new mock.repository.FakeUserQueryService();
        }
    }
}
