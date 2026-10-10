package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.TargetType;
import kgu.developers.domain.auditLog.infrastructure.AuditLogRepositoryImpl;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionRepositoryImpl;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.exception.TeamAlreadyConfirmedException;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.team.infrastructure.TeamRepositoryImpl;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberJpaEntity;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberRepositoryImpl;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:team-confirmation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;NON_KEYWORDS=YEAR",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TeamCommandService.class, TeamMemberCommandService.class, TeamQueryService.class,
    TeamRepositoryImpl.class, TeamMemberRepositoryImpl.class, SectionRepositoryImpl.class,
    AuditLogCommandService.class, AuditLogRepositoryImpl.class})
@Transactional(propagation = NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TeamMemberConfirmationIntegrationTest {

    @SpringBootConfiguration
    @EntityScan("kgu.developers")
    @EnableJpaRepositories("kgu.developers")
    static class TestConfig {
        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
    }

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private TeamCommandService teamCommandService;
    @Autowired
    private TeamMemberCommandService memberCommandService;
    @Autowired
    private TeamMemberRepositoryImpl memberRepository;
    @MockitoSpyBean
    private TeamRepositoryImpl teamRepository;
    @MockitoSpyBean
    private AuditLogRepositoryImpl auditLogRepository;

    private Long sectionId;
    private Long teamId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            UserJpaEntity user = UserJpaEntity.builder().studentNumber("202699999")
                .email("member@kgu.ac.kr").name("팀원").password("password")
                .globalRole(UserGlobalRole.USER).phone("01000000000").build();
            entityManager.persist(user);
            CourseJpaEntity course = CourseJpaEntity.builder().name("소프트웨어공학")
                .year(2026).semester(SemesterType.SPRING).status(StatusType.ACTIVE).build();
            entityManager.persist(course);
            SectionJpaEntity section = SectionJpaEntity.builder().professor(user).course(course)
                .code("SEC-01").name("1분반").classTime("월 1-3").capacity(30).build();
            entityManager.persist(section);
            TeamJpaEntity team = TeamJpaEntity.builder().section(section).name("1팀")
                .kickoffRule("규칙").meetingSchedule("매주 월요일").status(Status.FORMING).build();
            entityManager.persist(team);
            TeamMemberJpaEntity member = TeamMemberJpaEntity.builder().team(team).user(user)
                .isLeader(false).projectRole("백엔드").build();
            entityManager.persist(member);
            entityManager.flush();
            sectionId = section.getId();
            teamId = team.getId();
            memberId = member.getId();
        });
    }

    @Test
    @DisplayName("확정 로그는 행위자·시각·상태를 저장하고 재요청에도 중복되지 않는다")
    void confirmationAuditIsPersistedOnce() {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        teamCommandService.finalizeTeams(sectionId, "another-admin");

        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged()))
                .singleElement().satisfies(log -> {
                    assertThat(log.getActorId()).isEqualTo("202699999");
                    assertThat(log.getCreatedAt()).isNotNull();
                    assertThat(log.getEventType()).isEqualTo(AuditLogEventType.TEAM_UPDATED);
                    assertThat(log.getTargetType()).isEqualTo(TargetType.TEAM);
                    assertThat(log.getMetadata().path("changeType").asText()).isEqualTo("TEAM_FINALIZED");
                    assertThat(log.getMetadata().path("before").path("status").asText()).isEqualTo("FORMING");
                    assertThat(log.getMetadata().path("after").path("status").asText()).isEqualTo("CONFIRMED");
                });
        });
    }

    @Test
    @DisplayName("감사 로그 저장 후 실패해도 팀 확정과 로그가 함께 롤백된다")
    void confirmationRollsBackWithAuditFailure() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("audit failure");
        }).when(auditLogRepository).save(any());

        assertThatThrownBy(() -> teamCommandService.finalizeTeams(sectionId, "202699999"))
            .isInstanceOf(IllegalStateException.class).hasMessage("audit failure");

        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.FORMING);
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged())).isEmpty();
        });
    }

    @Test
    @DisplayName("역할 변경은 실제 확정 트랜잭션의 팀 행 잠금을 기다린 뒤 확정 상태로 거절된다")
    void concurrentTeamConfirmationAndMemberUpdate() throws Exception {
        CountDownLatch confirmationSaved = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CountDownLatch updateAttempted = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object saved = invocation.callRealMethod();
            // 실제 확정 경로가 저장한 뒤에도 트랜잭션과 DB 행 잠금을 유지한다.
            confirmationSaved.countDown();
            assertThat(allowCommit.await(5, TimeUnit.SECONDS)).isTrue();
            return saved;
        }).when(teamRepository).save(any());
        doAnswer(invocation -> {
            if (confirmationSaved.getCount() == 0) {
                updateAttempted.countDown();
            }
            return invocation.callRealMethod();
        }).when(teamRepository).findByIdForUpdate(teamId);

        var executor = Executors.newFixedThreadPool(2);
        try {
            var confirmation = executor.submit(() -> teamCommandService.finalizeTeams(sectionId, "202699999"));
            assertThat(confirmationSaved.await(5, TimeUnit.SECONDS)).isTrue();
            var update = executor.submit(() -> {
                assertThatThrownBy(() -> tx.executeWithoutResult(status ->
                    memberCommandService.updateTeamMember(memberRepository.findById(memberId).orElseThrow(),
                        null, "프론트엔드", null)))
                    .isInstanceOf(TeamAlreadyConfirmedException.class);
            });
            assertThat(updateAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> update.get(300, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

            allowCommit.countDown();
            assertThat(confirmation.get(5, TimeUnit.SECONDS)).singleElement()
                .satisfies(team -> assertThat(team.getStatus()).isEqualTo(Status.CONFIRMED));
            update.get(5, TimeUnit.SECONDS);

            tx.executeWithoutResult(status -> {
                assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.CONFIRMED);
                var member = memberRepository.findById(memberId).orElseThrow();
                assertThat(member.getProjectRole()).isEqualTo("백엔드");
                assertThat(member.isLeader()).isFalse();
            });
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
