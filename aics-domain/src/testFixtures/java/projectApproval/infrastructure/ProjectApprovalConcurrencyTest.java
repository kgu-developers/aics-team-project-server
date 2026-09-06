package projectApproval.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.projectApproval.infrastructure.JpaProjectApprovalRepository;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalJpaEntity;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalRepositoryImpl;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;

/**
 * 실제 DB(H2, PostgreSQL 모드)에 두 트랜잭션을 동시에 태워 동의 생성 경쟁을 검증한다.
 */
@SpringBootTest(
    classes = ProjectApprovalConcurrencyTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:project_approval;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
    })
class ProjectApprovalConcurrencyTest {

    private static final Long PROJECT_ID = 1L;
    private static final String STUDENT_NUMBER = "20260001";

    @Autowired
    private ProjectApprovalCommandService projectApprovalCommandService;

    @Autowired
    private JpaProjectApprovalRepository jpaProjectApprovalRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        jpaProjectApprovalRepository.deleteAll();
        Mockito.reset(projectRepository, userRepository);
        given(projectRepository.findByIdForUpdate(PROJECT_ID))
            .willReturn(Optional.of(Project.builder().id(PROJECT_ID).build()));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER))
            .willReturn(Optional.of(User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "encoded",
                UserGlobalRole.USER, "010-1234-5678")));
    }

    @AfterEach
    void tearDown() {
        jpaProjectApprovalRepository.deleteAll();
    }

    @Test
    @DisplayName("삭제된 동의를 동시에 재활성화하면 한 요청만 성공한다")
    void reactivateConcurrently() throws Exception {
        LocalDateTime deletedAt = LocalDateTime.now().minusDays(1);
        ProjectApprovalJpaEntity deleted = ProjectApprovalJpaEntity.builder()
            .projectId(PROJECT_ID)
            .userId(STUDENT_NUMBER)
            .proposalRevision(0L)
            .approvedAt(deletedAt)
            .build();
        deleted.setDeletedAt(deletedAt);
        jpaProjectApprovalRepository.saveAndFlush(deleted);

        List<Throwable> failures = runConcurrently();

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(DuplicateProjectApprovalException.class);
        assertThat(activeApprovals()).hasSize(1);
    }

    @Test
    @DisplayName("동의 이력이 없을 때 동시에 생성하면 한 요청만 성공한다")
    void createConcurrently() throws Exception {
        List<Throwable> failures = runConcurrently();

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(DuplicateProjectApprovalException.class);
        assertThat(activeApprovals()).hasSize(1);
    }

    private List<Throwable> runConcurrently() throws InterruptedException {
        int threads = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        projectApprovalCommandService.approve(
                            PROJECT_ID, STUDENT_NUMBER, LocalDateTime.now());
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

    private List<ProjectApprovalJpaEntity> activeApprovals() {
        return jpaProjectApprovalRepository.findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(PROJECT_ID);
    }

    @Configuration
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class
    })
    // countApprovalsByTeamMembers 가 TeamMemberJpaEntity 를 조인하므로 엔티티는 전부 스캔한다.
    @EntityScan("kgu.developers")
    @EnableJpaRepositories(basePackageClasses = JpaProjectApprovalRepository.class)
    @Import(ProjectApprovalRepositoryImpl.class)
    static class TestConfig {

        @Bean
        ProjectApprovalCommandService projectApprovalCommandService(
            ProjectApprovalRepository projectApprovalRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {
            return new ProjectApprovalCommandService(projectApprovalRepository, projectRepository, userRepository);
        }

        @Bean
        ProjectRepository projectRepository() {
            return Mockito.mock(ProjectRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }
    }
}
