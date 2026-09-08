package project.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import kgu.developers.common.exception.CustomException;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.course.domain.SemesterType;
import com.fasterxml.jackson.databind.ObjectMapper;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.infrastructure.ProjectJpaEntity;
import kgu.developers.domain.project.infrastructure.ProjectRepositoryImpl;
import kgu.developers.domain.project.infrastructure.ProposalSectionRepositoryImpl;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalJpaEntity;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalRepositoryImpl;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberRepositoryImpl;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

/**
 * 주제 확정이 제안서 갱신 경로(리비전 증가·동의 무효화·완료 제안서 보호)를 실제로 타는지
 * PostgreSQL 위에서 검증한다.
 */
@DataJpaTest
@Testcontainers
@Import({ProjectCommandService.class, ProjectRepositoryImpl.class, ProjectApprovalRepositoryImpl.class,
    ProposalSectionRepositoryImpl.class, TeamMemberRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(propagation = NOT_SUPPORTED)
class ProjectTopicFinalizeIntegrationTest {

    private static final Long FIRST_CANDIDATE_ID = 100L;
    private static final Long SECOND_CANDIDATE_ID = 200L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @SpringBootConfiguration
    @EntityScan("kgu.developers")
    @EnableJpaRepositories("kgu.developers")
    static class TestConfig {
    }

    @Autowired
    private ProjectCommandService projectCommandService;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TransactionTemplate tx;
    private Long teamId;

    @Autowired
    void setTransactionTemplate(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(status -> {
            entityManager.createQuery("delete from ProposalSectionJpaEntity").executeUpdate();
            entityManager.createQuery("delete from ProjectApprovalJpaEntity").executeUpdate();
            entityManager.createQuery("delete from ProjectJpaEntity").executeUpdate();
            entityManager.createQuery("delete from TeamJpaEntity").executeUpdate();
            entityManager.createQuery("delete from SectionJpaEntity").executeUpdate();
            entityManager.createQuery("delete from CourseJpaEntity").executeUpdate();
            entityManager.createQuery("delete from UserJpaEntity").executeUpdate();

            UserJpaEntity professor = UserJpaEntity.builder()
                .studentNumber("202000001")
                .email("professor@kgu.ac.kr")
                .name("교수")
                .password("password")
                .globalRole(UserGlobalRole.ADMIN)
                .phone("01000000000")
                .build();
            entityManager.persist(professor);

            CourseJpaEntity course = CourseJpaEntity.builder()
                .name("소프트웨어공학")
                .year(2026)
                .semester(SemesterType.SPRING)
                .status(StatusType.ACTIVE)
                .build();
            entityManager.persist(course);

            SectionJpaEntity section = SectionJpaEntity.builder()
                .professor(professor)
                .course(course)
                .code("SEC-01")
                .name("1분반")
                .classTime("월 1-3")
                .capacity(30)
                .build();
            entityManager.persist(section);

            TeamJpaEntity team = TeamJpaEntity.builder()
                .section(section)
                .name("1팀")
                .kickoffRule("규칙")
                .meetingSchedule("매주 월요일")
                .status(Status.FORMING)
                .build();
            entityManager.persist(team);
            entityManager.flush();

            teamId = team.getId();
        });
    }

    @Test
    @DisplayName("최초 주제 확정은 프로젝트를 DRAFT로 만들고 topicCandidateId를 남긴다")
    void finalizeTopic_createsProjectWithTopicCandidateLink() {
        Project created = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "첫 주제", "첫 설명", "첫 목표"));

        assertThat(created.getTopicCandidateId()).isEqualTo(FIRST_CANDIDATE_ID);
        assertThat(created.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(created.getProposalRevision()).isZero();
        assertThat(reload(created.getId()).getTopicCandidateId()).isEqualTo(FIRST_CANDIDATE_ID);
    }

    @Test
    @DisplayName("주제를 바꾸면 리비전이 오르고 기존 동의가 무효화되며 새 후보로 연결된다")
    void finalizeTopic_bumpsRevisionAndClearsApprovals() {
        Project created = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "첫 주제", "첫 설명", "첫 목표"));
        approve(created.getId(), "202412345", created.getProposalRevision());

        Project changed = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, SECOND_CANDIDATE_ID, "두 번째 주제", "두 번째 설명", "두 번째 목표"));

        assertThat(changed.getId()).isEqualTo(created.getId());
        assertThat(changed.getProposalRevision()).isEqualTo(1L);
        assertThat(changed.getTopicCandidateId()).isEqualTo(SECOND_CANDIDATE_ID);
        assertThat(changed.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);

        ProjectJpaEntity persisted = reload(created.getId());
        assertThat(persisted.getTitle()).isEqualTo("두 번째 주제");
        assertThat(persisted.getProposalRevision()).isEqualTo(1L);
        assertThat(persisted.getTopicCandidateId()).isEqualTo(SECOND_CANDIDATE_ID);
        assertThat(activeApprovalCount(created.getId())).isZero();
    }

    @Test
    @DisplayName("제안 완료된 프로젝트는 주제를 다시 확정할 수 없다")
    void finalizeTopic_rejectsCompletedProposal() {
        Project created = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "첫 주제", "첫 설명", "첫 목표"));
        approve(created.getId(), "202412345", created.getProposalRevision());
        completeProposal(created.getId());

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> projectCommandService.finalizeTopic(
            teamId, SECOND_CANDIDATE_ID, "두 번째 주제", "두 번째 설명", "두 번째 목표")))
            .isInstanceOf(CustomException.class);

        ProjectJpaEntity persisted = reload(created.getId());
        assertThat(persisted.getTitle()).isEqualTo("첫 주제");
        assertThat(persisted.getTopicCandidateId()).isEqualTo(FIRST_CANDIDATE_ID);
        assertThat(activeApprovalCount(created.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("제안서 수정으로 채운 협업방식·저장소·데이터/화면 구성·일정은 주제 확정 후에도 유지된다")
    void finalizeTopic_keepsProposalFieldsOutsideTopic() {
        Project created = tx.execute(status -> projectCommandService.saveProject(
            teamId, "첫 주제", "첫 설명", "첫 목표", "대면", "https://github.com/kgu/project", null,
            kgu.developers.common.json.JsonConverter.parse("[{\"name\":\"학습 로그\",\"description\":\"문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]"),
            JsonConverter.parse("[{\"title\":\"홈\",\"description\":\"요약\",\"imageFileId\":1}]"), "4월: 설계, 5월: 개발, 6월: 통합 테스트"));

        tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "두 번째 주제", "두 번째 설명", "두 번째 목표"));

        ProjectJpaEntity persisted = reload(created.getId());
        assertThat(persisted.getCollaborationStyle()).isEqualTo("대면");
        assertThat(persisted.getRepositoryUrl()).isEqualTo("https://github.com/kgu/project");
        assertThat(persisted.getTopicCandidateId()).isEqualTo(FIRST_CANDIDATE_ID);
        assertThat(persisted.getDataConfiguration()).isEqualTo(objectMapper.createArrayNode());
        assertThat(persisted.getScreenConfiguration().get(0).get("title").asText()).isEqualTo("홈");
        assertThat(persisted.getProjectSchedule()).isEqualTo("4월: 설계, 5월: 개발, 6월: 통합 테스트");
    }

    @Test
    @DisplayName("같은 내용을 다시 확정하면 리비전과 동의를 건드리지 않는다")
    void finalizeTopic_isIdempotentForSameContent() {
        Project created = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "첫 주제", "첫 설명", "첫 목표"));
        approve(created.getId(), "202412345", created.getProposalRevision());

        Project again = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "첫 주제", "첫 설명", "첫 목표"));

        assertThat(again.getProposalRevision()).isZero();
        assertThat(again.getTopicCandidateId()).isEqualTo(FIRST_CANDIDATE_ID);
        assertThat(activeApprovalCount(created.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제된 프로젝트에 주제를 확정하면 새 리비전으로 되살아나고 이전 동의는 무효화된다")
    void finalizeTopic_reactivatesDeletedProject() {
        Project created = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, FIRST_CANDIDATE_ID, "첫 주제", "첫 설명", "첫 목표"));
        approve(created.getId(), "202412345", created.getProposalRevision());
        tx.executeWithoutResult(status -> projectCommandService.deleteProject(created.getId()));

        Project revived = tx.execute(status -> projectCommandService.finalizeTopic(
            teamId, SECOND_CANDIDATE_ID, "두 번째 주제", "두 번째 설명", "두 번째 목표"));

        assertThat(revived.getId()).isEqualTo(created.getId());
        assertThat(revived.getProposalRevision()).isEqualTo(1L);
        assertThat(revived.getTopicCandidateId()).isEqualTo(SECOND_CANDIDATE_ID);

        ProjectJpaEntity persisted = reload(created.getId());
        assertThat(persisted.getDeletedAt()).isNull();
        assertThat(persisted.getTopicCandidateId()).isEqualTo(SECOND_CANDIDATE_ID);
        assertThat(activeApprovalCount(created.getId())).isZero();
    }

    private ProjectJpaEntity reload(Long projectId) {
        return tx.execute(status -> entityManager.find(ProjectJpaEntity.class, projectId));
    }

    private void approve(Long projectId, String userId, long proposalRevision) {
        tx.executeWithoutResult(status -> entityManager.persist(ProjectApprovalJpaEntity.builder()
            .projectId(projectId)
            .userId(userId)
            .proposalRevision(proposalRevision)
            .approvedAt(java.time.LocalDateTime.now())
            .build()));
    }

    private long activeApprovalCount(Long projectId) {
        return tx.execute(status -> entityManager.createQuery(
                "select count(a) from ProjectApprovalJpaEntity a where a.projectId = :projectId and a.deletedAt is null",
                Long.class)
            .setParameter("projectId", projectId)
            .getSingleResult());
    }

    private void completeProposal(Long projectId) {
        tx.executeWithoutResult(status -> entityManager.createQuery(
                "update ProjectJpaEntity p set p.proposalCompletedAt = :now where p.id = :id")
            .setParameter("now", java.time.LocalDateTime.now())
            .setParameter("id", projectId)
            .executeUpdate());
    }
}
