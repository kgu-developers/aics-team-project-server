package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.TargetType;
import kgu.developers.domain.auditLog.infrastructure.AuditLogRepositoryImpl;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationJpaEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordJpaEntity;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.infrastructure.ProjectJpaEntity;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionRepositoryImpl;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
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
    @Test
    @DisplayName("취소는 확정 팀만 변경하며 재요청에도 팀별 감사 로그가 중복되지 않는다")
    void unfinalizationAuditIsPersistedOnce() {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        Long formingTeamId = tx.execute(status -> {
            TeamJpaEntity team = TeamJpaEntity.builder()
                .section(entityManager.getReference(SectionJpaEntity.class, sectionId))
                .name("2팀").kickoffRule("규칙").meetingSchedule("매주 월요일").status(Status.FORMING).build();
            entityManager.persist(team);
            entityManager.flush();
            return team.getId();
        });
        assertThat(teamCommandService.unfinalizeTeams(sectionId, "202699999"))
            .hasSize(2).allSatisfy(team -> assertThat(team.getStatus()).isEqualTo(Status.FORMING));
        teamCommandService.unfinalizeTeams(sectionId, "another-admin");

        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(auditLogRepository.findAllByTeam(sectionId, formingTeamId, Pageable.unpaged())).isEmpty();
            var logs = auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged());
            assertThat(logs).hasSize(2);
            assertThat(logs.stream().filter(log -> log.getMetadata().path("changeType").asText()
                .equals("TEAM_UNFINALIZED"))).singleElement().satisfies(log -> {
                    assertThat(log.getActorId()).isEqualTo("202699999");
                    assertThat(log.getCreatedAt()).isNotNull();
                    assertThat(log.getEventType()).isEqualTo(AuditLogEventType.TEAM_UPDATED);
                    assertThat(log.getTargetType()).isEqualTo(TargetType.TEAM);
                    assertThat(log.getMetadata().path("before").path("status").asText()).isEqualTo("CONFIRMED");
                    assertThat(log.getMetadata().path("after").path("status").asText()).isEqualTo("FORMING");
                    assertThat(log.getMetadata().has("reason")).isFalse();
                });
        });
    }

    @Test
    @DisplayName("취소 로그 저장 실패 시 해당 팀 상태와 로그가 함께 롤백된다")
    void unfinalizationRollsBackWithAuditFailure() {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("audit failure");
        }).when(auditLogRepository).save(any());

        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999"))
            .isInstanceOf(IllegalStateException.class).hasMessage("audit failure");
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged())).singleElement()
                .satisfies(log -> assertThat(log.getMetadata().path("changeType").asText()).isEqualTo("TEAM_FINALIZED"));
        });
    }

    @Test
    @DisplayName("없는 분반 취소는 상태와 로그를 변경하지 않는다")
    void unfinalizationRejectsMissingSection() {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(Long.MAX_VALUE, "202699999"))
            .isInstanceOf(SectionNotFoundException.class);
        tx.executeWithoutResult(status -> {
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged())).hasSize(1);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("역할 변경과 재확정은 취소 트랜잭션의 행 잠금을 기다린 뒤 최신 상태로 처리된다")
    void concurrentUnfinalization(boolean finalizeAgain) throws Exception {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        CountDownLatch cancellationSaved = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CountDownLatch competingAttempted = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object saved = invocation.callRealMethod();
            if (((Team) invocation.getArgument(0)).getStatus() == Status.FORMING) {
                cancellationSaved.countDown();
                assertThat(allowCommit.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return saved;
        }).when(teamRepository).save(any());
        doAnswer(invocation -> {
            if (cancellationSaved.getCount() == 0) {
                competingAttempted.countDown();
            }
            return invocation.callRealMethod();
        }).when(teamRepository).findByIdForUpdate(teamId);

        var executor = Executors.newFixedThreadPool(2);
        try {
            var cancellation = executor.submit(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999"));
            assertThat(cancellationSaved.await(5, TimeUnit.SECONDS)).isTrue();
            var competing = executor.submit(() -> {
                if (finalizeAgain) {
                    teamCommandService.finalizeTeams(sectionId, "202699999");
                } else {
                    memberCommandService.updateTeamMember(memberRepository.findById(memberId).orElseThrow(),
                        null, "프론트엔드", null);
                }
            });
            assertThat(competingAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> competing.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            allowCommit.countDown();
            cancellation.get(5, TimeUnit.SECONDS);
            competing.get(5, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                entityManager.clear();
                assertThat(teamRepository.findById(teamId).orElseThrow().getStatus())
                    .isEqualTo(finalizeAgain ? Status.CONFIRMED : Status.FORMING);
                assertThat(memberRepository.findById(memberId).orElseThrow().getProjectRole())
                    .isEqualTo(finalizeAgain ? "백엔드" : "프론트엔드");
                assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged()))
                    .hasSize(finalizeAgain ? 3 : 2);
            });
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("취소는 팀원·제안서·회의록·평가를 보존하고 이후 팀 이동과 역할 변경을 허용한다")
    void unfinalizationPreservesDataAndAllowsMemberMove() {
        Long[] ids = tx.execute(status -> {
            var team = entityManager.find(TeamJpaEntity.class, teamId);
            var project = ProjectJpaEntity.builder()
                .team(team).title("제안서").description("설명").goal("목표")
                .approvalStatus(ApprovalStatus.APPROVED).build();
            entityManager.persist(project);
            var meeting = MeetingRecordJpaEntity.builder()
                .teamId(teamId).title("회의").phase(MeetingPhase.PROPOSAL)
                .authorId("202699999").meetingAt(LocalDateTime.of(2026, 10, 10, 10, 0))
                .content("회의 내용").build();
            entityManager.persist(meeting);
            var evaluation = TeamEvaluationJpaEntity.builder()
                .milestoneId(1L).raterId("202699999").rateeTeamId(teamId)
                .submittedAt(LocalDateTime.of(2026, 10, 10, 11, 0)).build();
            entityManager.persist(evaluation);
            var target = TeamJpaEntity.builder().section(team.getSection()).name("2팀").kickoffRule("규칙").meetingSchedule("매주 월요일").status(Status.FORMING).build();
            entityManager.persist(target);
            entityManager.flush();
            return new Long[]{project.getId(), meeting.getId(), evaluation.getId(), target.getId()};
        });
        teamCommandService.finalizeTeams(sectionId, "202699999");
        teamCommandService.unfinalizeTeams(sectionId, "202699999");
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            var member = memberRepository.findById(memberId).orElseThrow();
            assertThat(member.getTeamId()).isEqualTo(teamId);
            assertThat(member.getProjectRole()).isEqualTo("백엔드");
            assertThat(member.isLeader()).isFalse();
            var project = entityManager.find(ProjectJpaEntity.class, ids[0]);
            assertThat(project.getTeam().getId()).isEqualTo(teamId);
            assertThat(project.getTitle()).isEqualTo("제안서");
            assertThat(project.getDescription()).isEqualTo("설명");
            assertThat(project.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            var meeting = entityManager.find(MeetingRecordJpaEntity.class, ids[1]);
            assertThat(meeting.getTeamId()).isEqualTo(teamId);
            assertThat(meeting.getContent()).isEqualTo("회의 내용");
            var evaluation = entityManager.find(TeamEvaluationJpaEntity.class, ids[2]);
            assertThat(evaluation.getRateeTeamId()).isEqualTo(teamId);
            assertThat(evaluation.getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 10, 10, 11, 0));
            memberCommandService.updateTeamMember(member, ids[3], "프론트엔드", null);
        });
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            var member = memberRepository.findById(memberId).orElseThrow();
            assertThat(member.getTeamId()).isEqualTo(ids[3]);
            assertThat(member.getProjectRole()).isEqualTo("프론트엔드");
        });
    }

}
