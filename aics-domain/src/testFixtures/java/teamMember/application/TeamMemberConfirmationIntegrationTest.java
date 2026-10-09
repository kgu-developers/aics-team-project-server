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

import jakarta.persistence.EntityManager;
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
    TeamRepositoryImpl.class, TeamMemberRepositoryImpl.class, SectionRepositoryImpl.class})
@Transactional(propagation = NOT_SUPPORTED)
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
            var confirmation = executor.submit(() -> teamCommandService.finalizeTeams(sectionId));
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
