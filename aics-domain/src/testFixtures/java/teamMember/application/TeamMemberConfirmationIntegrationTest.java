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

import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.infrastructure.MilestoneJpaEntity;
import kgu.developers.domain.milestone.infrastructure.MilestoneRepositoryImpl;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.infrastructure.SubmissionJpaEntity;
import kgu.developers.domain.submission.infrastructure.SubmissionRepositoryImpl;
import kgu.developers.domain.submission.infrastructure.SubmissionMemberConfirmationRepositoryImpl;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import kgu.developers.domain.enrollment.infrastructure.EnrollmentRepositoryImpl;
import kgu.developers.domain.enrollment.infrastructure.EnrollmentJpaEntity;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.team.exception.TeamUnfinalizeCompletedFinalReportException;
import kgu.developers.domain.submission.exception.SubmissionTeamNotConfirmedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
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
import kgu.developers.domain.team.exception.TeamInvalidUnfinalizeReasonException;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.team.infrastructure.TeamRepositoryImpl;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberJpaEntity;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberRepositoryImpl;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:team-confirmation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;NON_KEYWORDS=YEAR",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "aws.s3.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TeamCommandService.class, TeamMemberCommandService.class, TeamQueryService.class,
    TeamRepositoryImpl.class, TeamMemberRepositoryImpl.class, SectionRepositoryImpl.class,
    AuditLogCommandService.class, AuditLogRepositoryImpl.class, MilestoneRepositoryImpl.class,
    SubmissionRepositoryImpl.class, SubmissionCommandService.class, SubmissionQueryService.class,
    SubmissionMemberConfirmationRepositoryImpl.class, EnrollmentRepositoryImpl.class})
@Transactional(propagation = NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TeamMemberConfirmationIntegrationTest {

    private static final String REASON = "팀 배정 오류를 수정하고 학생들의 팀 구성을 다시 조정하기 위해 취소합니다.";

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
    private TeamQueryService teamQueryService;
    @Autowired
    private TeamMemberCommandService memberCommandService;
    @Autowired
    private TeamMemberRepositoryImpl memberRepository;
    @MockitoSpyBean
    private TeamRepositoryImpl teamRepository;
    @MockitoSpyBean
    private AuditLogRepositoryImpl auditLogRepository;

    @Autowired
    private SubmissionCommandService submissionCommandService;
    @Autowired
    private SubmissionRepositoryImpl submissionRepository;
    @Autowired
    private SubmissionMemberConfirmationRepositoryImpl confirmationRepository;
    @MockitoSpyBean
    private SectionRepositoryImpl sectionRepository;
    @MockitoBean
    private SubmissionVersionRepository versionRepository;
    @MockitoBean
    private SubmissionArtifactRepository artifactRepository;
    @MockitoBean
    private FileObjectRepository fileObjectRepository;
    @MockitoBean
    private FileStorage fileStorage;
    @MockitoBean
    private RequiredArtifactRepository requiredArtifactRepository;

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
        assertThat(teamCommandService.unfinalizeTeams(sectionId, "202699999", "  " + REASON + "  "))
            .hasSize(2).allSatisfy(team -> assertThat(team.getStatus()).isEqualTo(Status.FORMING));
        teamCommandService.unfinalizeTeams(sectionId, "another-admin", REASON);

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
                    assertThat(log.getMetadata().path("reason").asText()).isEqualTo(REASON);
                });
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "짧은 사유"})
    @DisplayName("잘못된 취소 사유는 상태와 로그를 변경하지 않는다")
    void invalidUnfinalizationReasonChangesNothing(String reason) {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", reason))
            .isInstanceOf(TeamInvalidUnfinalizeReasonException.class);
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged())).hasSize(1);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {29, 30, 500, 501})
    @DisplayName("직접 취소 호출도 공백 제거 후 30~500자만 허용하고 저장한다")
    void unfinalizationReasonLength(int length) {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        String reason = "가".repeat(length);
        boolean valid = length >= 30 && length <= 500;
        if (valid) {
            teamCommandService.unfinalizeTeams(sectionId, "202699999", "  " + reason + "  ");
        } else {
            assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", "  " + reason + "  "))
                .isInstanceOf(TeamInvalidUnfinalizeReasonException.class);
        }
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus())
                .isEqualTo(valid ? Status.FORMING : Status.CONFIRMED);
            var logs = auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged());
            assertThat(logs).hasSize(valid ? 2 : 1);
            if (valid) {
                assertThat(logs.stream().filter(log -> log.getMetadata().path("changeType").asText()
                    .equals("TEAM_UNFINALIZED"))).singleElement().satisfies(log ->
                        assertThat(log.getMetadata().path("reason").asText()).isEqualTo(reason));
            }
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

        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON))
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
        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(Long.MAX_VALUE, "202699999", REASON))
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
            var cancellation = executor.submit(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON));
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
        teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON);
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

    @Test
    @DisplayName("완료된 최종보고서가 하나라도 있으면 분반 전체 취소를 거부한다")
    void completedFinalReportBlocksUnfinalization() {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        Long secondTeam = createTeam("2팀", Status.CONFIRMED);
        createSubmission(secondTeam, MilestoneType.FINAL_REPORT, SubmissionStatus.COMPLETED, false);
        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON))
            .isInstanceOf(TeamUnfinalizeCompletedFinalReportException.class);
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged())).hasSize(1);
        });
    }

    @ParameterizedTest
    @CsvSource({"FINAL_REPORT,SUBMITTED,false", "FINAL_REPORT,NOT_SUBMITTED,false",
        "FINAL_REPORT,REVISION_REQUESTED,false", "GENERAL,COMPLETED,false", "FINAL_REPORT,COMPLETED,true"})
    @DisplayName("미완료·재개된 최종보고서와 일반 제출 완료·삭제된 제출은 취소를 막지 않는다")
    void unfinalizationAllowsOtherSubmissionStates(MilestoneType type, SubmissionStatus state, boolean deleted) {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        createSubmission(teamId, type, state, deleted);
        teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON);
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.FORMING);
            assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged())).hasSize(2);
        });
    }

    @Test
    @DisplayName("최종보고서를 실제로 재개한 후 확정 취소가 가능하다")
    void reopenedFinalReportAllowsUnfinalization() {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        Long id = createSubmission(teamId, MilestoneType.FINAL_REPORT, SubmissionStatus.COMPLETED, false);
        submissionCommandService.reopenSubmission(id, "202699999", LocalDateTime.now().plusDays(1));
        teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON);
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(submissionRepository.findById(id).orElseThrow().getStatus()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
            assertThat(teamRepository.findById(teamId).orElseThrow().getStatus()).isEqualTo(Status.FORMING);
        });
    }

    @Test
    @DisplayName("두 번째 팀 취소 로그 실패 시 앞선 팀도 함께 롤백한다")
    void unfinalizationRollsBackEntireSection() {
        Long secondId = createTeam("2팀", Status.FORMING);
        teamCommandService.finalizeTeams(sectionId, "202699999");
        doAnswer(invocation -> {
            var log = (kgu.developers.domain.auditLog.domain.AuditLog) invocation.getArgument(0);
            invocation.callRealMethod();
            if (log.getTargetId().equals(secondId)) {
                throw new IllegalStateException("second audit failure");
            }
            return log;
        }).when(auditLogRepository).save(any());
        assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON))
            .isInstanceOf(IllegalStateException.class).hasMessage("second audit failure");
        tx.executeWithoutResult(status -> {
            entityManager.clear();
            for (Long id : java.util.List.of(teamId, secondId)) {
                assertThat(teamRepository.findById(id).orElseThrow().getStatus()).isEqualTo(Status.CONFIRMED);
                assertThat(auditLogRepository.findAllByTeam(sectionId, id, Pageable.unpaged())).hasSize(1);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("완료와 취소는 분반 잠금을 기다리고 먼저 커밋한 상태에 따라 후속 작업을 거부한다")
    void completionAndUnfinalizationSerialize(boolean completionFirst) throws Exception {
        teamCommandService.finalizeTeams(sectionId, "202699999");
        Long submissionId = createSubmission(teamId, MilestoneType.FINAL_REPORT, SubmissionStatus.SUBMITTED, false);
        tx.executeWithoutResult(status -> {
            entityManager.persist(EnrollmentJpaEntity.builder()
                .sectionId(sectionId).userId("202699999")
                .role(Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.ACTIVE).build());
            confirmationRepository.save(SubmissionMemberConfirmation.builder().submissionId(submissionId)
                .userId("202699999").version(1).confirmedAt(LocalDateTime.now()).build());
        });
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch waiting = new CountDownLatch(1);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            int call = calls.incrementAndGet();
            if (call == 2) {
                waiting.countDown();
            }
            Object result = invocation.callRealMethod();
            if (call == 1) {
                locked.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return result;
        }).when(sectionRepository).findActiveByIdForUpdate(sectionId);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                if (completionFirst) {
                    submissionCommandService.completeSubmission(submissionId, "202699999");
                } else {
                    teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON);
                }
            });
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                if (completionFirst) {
                    assertThatThrownBy(() -> teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON))
                        .isInstanceOf(TeamUnfinalizeCompletedFinalReportException.class);
                } else {
                    assertThatThrownBy(() -> submissionCommandService.completeSubmission(submissionId, "202699999"))
                        .isInstanceOf(SubmissionTeamNotConfirmedException.class);
                }
            });
            assertThat(waiting.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                entityManager.clear();
                assertThat(teamRepository.findById(teamId).orElseThrow().getStatus())
                    .isEqualTo(completionFirst ? Status.CONFIRMED : Status.FORMING);
                assertThat(submissionRepository.findById(submissionId).orElseThrow().getStatus())
                    .isEqualTo(completionFirst ? SubmissionStatus.COMPLETED : SubmissionStatus.SUBMITTED);
                assertThat(auditLogRepository.findAllByTeam(sectionId, teamId, Pageable.unpaged()))
                    .hasSize(completionFirst ? 1 : 2);
            });
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("잠금 조회는 앞서 읽은 제출도 다른 트랜잭션이 커밋한 최신 버전으로 갱신한다")
    void lockedSubmissionRefreshesPreviouslyLoadedEntity() {
        Long id = createSubmission(teamId, MilestoneType.FINAL_REPORT, SubmissionStatus.SUBMITTED, false);
        tx.executeWithoutResult(status -> {
            assertThat(submissionRepository.findById(id).orElseThrow().getCurrentVersion()).isEqualTo(1);
            var independent = new TransactionTemplate(tx.getTransactionManager());
            independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            independent.executeWithoutResult(inner -> {
                var submission = submissionRepository.findByIdForUpdate(id).orElseThrow();
                submission.recordNewVersion(2);
                submissionRepository.save(submission);
            });
            assertThat(submissionRepository.findByIdForUpdate(id).orElseThrow().getCurrentVersion()).isEqualTo(2);
        });
    }

    private Long createTeam(String name, Status status) {
        return tx.execute(transaction -> {
            var team = TeamJpaEntity.builder().section(entityManager.getReference(SectionJpaEntity.class, sectionId))
                .name(name).kickoffRule("규칙").meetingSchedule("매주 월요일").status(status).build();
            entityManager.persist(team);
            entityManager.flush();
            return team.getId();
        });
    }

    private Long createSubmission(Long ownerTeam, MilestoneType type, SubmissionStatus status, boolean deleted) {
        return tx.execute(transaction -> {
            var milestone = MilestoneJpaEntity.builder().sectionId(sectionId).title("보고서").weekNumber(15)
                .status(MilestoneStatus.PUBLISHED).type(type).dueAt(LocalDateTime.now().plusDays(1)).build();
            entityManager.persist(milestone);
            var submission = SubmissionJpaEntity.builder().teamId(ownerTeam).milestoneId(milestone.getId())
                .status(status).currentVersion(status == SubmissionStatus.NOT_SUBMITTED ? 0 : 1).build();
            if (deleted) {
                submission.setDeletedAt(LocalDateTime.now());
            }
            entityManager.persist(submission);
            entityManager.flush();
            return submission.getId();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("확정 취소와 팀명 수정은 시작 순서와 관계없이 교착 없이 완료된다")
    void cancellationAndKickoffUpdateUseSameLockOrder(boolean cancellationFirst) throws Exception {
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        var firstLock = new java.util.concurrent.atomic.AtomicBoolean(true);
        var executor = Executors.newFixedThreadPool(2);
        org.mockito.stubbing.Answer<Object> pauseFirstLock = invocation -> {
            Object result = invocation.callRealMethod();
            if (firstLock.compareAndSet(true, false)) {
                firstLocked.countDown();
                assertThat(releaseFirst.await(10, TimeUnit.SECONDS)).isTrue();
            }
            return result;
        };
        doAnswer(pauseFirstLock).when(teamRepository).findByIdForUpdate(teamId);
        doAnswer(pauseFirstLock).when(teamRepository).findByIdForNameUpdate(teamId);
        Runnable cancel = () -> teamCommandService.unfinalizeTeams(sectionId, "202699999", REASON);
        Runnable kickoff = () -> tx.executeWithoutResult(status -> {
            teamQueryService.getTeamByIdForNameUpdate(teamId);
            teamCommandService.updateKickoff(teamId, "변경한 팀명", "규칙", "일정");
        });
        try {
            var first = executor.submit(cancellationFirst ? cancel : kickoff);
            assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondStarted.countDown();
                (cancellationFirst ? kickoff : cancel).run();
            });
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                entityManager.clear();
                Team team = teamRepository.findById(teamId).orElseThrow();
                assertThat(team.getName()).isEqualTo("변경한 팀명");
                assertThat(team.getStatus()).isEqualTo(Status.FORMING);
            });
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("팀명 수정 잠금은 앞서 읽은 팀의 상태를 최신 확정 상태로 갱신한다")
    void nameUpdateLockRefreshesPreviouslyLoadedTeam() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            assertThat(teamQueryService.getTeamById(teamId).getStatus()).isEqualTo(Status.FORMING);
            var independent = new TransactionTemplate(tx.getTransactionManager());
            independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            independent.executeWithoutResult(inner -> teamCommandService.finalizeTeams(sectionId, "202699999"));
            assertThat(teamQueryService.getTeamByIdForNameUpdate(teamId).getStatus()).isEqualTo(Status.CONFIRMED);
            teamCommandService.updateKickoff(teamId, "변경한 팀명", "규칙", "일정");
        })).isInstanceOf(TeamAlreadyConfirmedException.class);
        tx.executeWithoutResult(status ->
            assertThat(teamRepository.findById(teamId).orElseThrow().getName()).isEqualTo("1팀"));
    }

    @Test
    @DisplayName("존재하지 않는 팀의 팀명 수정 잠금은 팀 없음 예외를 반환한다")
    void nameUpdateLockRejectsMissingTeam() {
        assertThatThrownBy(() -> teamQueryService.getTeamByIdForNameUpdate(Long.MAX_VALUE))
            .isInstanceOf(kgu.developers.domain.team.exception.TeamNotFoundException.class);
    }

}
