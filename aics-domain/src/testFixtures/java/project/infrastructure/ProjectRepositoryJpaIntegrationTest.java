package project.infrastructure;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectVersionConflictException;
import kgu.developers.domain.project.infrastructure.ProjectJpaEntity;
import kgu.developers.domain.project.infrastructure.ProjectRepositoryImpl;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

/**
 * 실제 PostgreSQL(Testcontainers) 위에서 트랜잭션을 분리해 @Version 증가와 충돌을 검증한다.
 */
@DataJpaTest
@Testcontainers
@Import(ProjectRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(propagation = NOT_SUPPORTED)
class ProjectRepositoryJpaIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // create-drop을 쓰면 컨테이너가 먼저 종료된 뒤 셧다운 훅에서 drop DDL을 시도하다
    // 커넥션 타임아웃(30초)을 기다린다. 컨테이너째 버려지므로 drop은 필요 없다.
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
  }

  @SpringBootConfiguration
  @EntityScan("kgu.developers")
  @EnableJpaRepositories("kgu.developers")
  static class TestConfig {
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private ProjectRepositoryImpl repository;

  @Autowired
  private EntityManager entityManager;

  private TransactionTemplate tx;
  private Long projectId;

  @Autowired
  void setTransactionTemplate(PlatformTransactionManager transactionManager) {
    this.tx = new TransactionTemplate(transactionManager);
  }

  @BeforeEach
  void setUp() {
    tx.executeWithoutResult(status -> {
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

      ProjectJpaEntity project = ProjectJpaEntity.builder()
          .team(team)
          .title("원본 프로젝트")
          .description("프로젝트 설명")
          .goal("프로젝트 목표")
          .repositoryUrl("https://github.com/example/repo")
          .externalLinks(externalLinks())
          .approvalStatus(ApprovalStatus.APPROVED)
          .meetingStyle("온라인")
          .build();
      entityManager.persist(project);
      entityManager.flush();

      projectId = project.getId();
    });
  }

  @Test
  @DisplayName("조회 → 삭제 → 오래된 객체 저장 시 ProjectVersionConflictException이 발생한다")
  void readDeleteThenSaveOldObject() {
    // 트랜잭션 1: 프로젝트 조회 (영속성 컨텍스트 종료 후 준영속 객체로 보관)
    Project stale = tx.execute(status -> repository.findById(projectId).orElseThrow());
    assertThat(stale.getVersion()).isZero();

    // 트랜잭션 2: 프로젝트 삭제 (@Version 증가)
    repository.deleteById(projectId);

    ProjectJpaEntity afterDelete = tx.execute(status ->
        entityManager.find(ProjectJpaEntity.class, projectId));
    assertThat(afterDelete.getDeletedAt()).isNotNull();
    assertThat(afterDelete.getVersion()).isEqualTo(1L);

    // 트랜잭션 3: 트랜잭션 1에서 읽은 오래된 객체로 저장 시도
    Project oldProject = Project.builder()
        .id(stale.getId())
        .teamId(stale.getTeamId())
        .title("수정된 제목")
        .description(stale.getDescription())
        .goal(stale.getGoal())
        .repositoryUrl(stale.getRepositoryUrl())
        .externalLinks(stale.getExternalLinks())
        .approvalStatus(stale.getApprovalStatus())
        .meetingStyle(stale.getMeetingStyle())
        .createdAt(stale.getCreatedAt())
        .version(stale.getVersion())
        .build();

    assertThatThrownBy(() -> repository.save(oldProject))
        .isInstanceOf(ProjectVersionConflictException.class);
  }

  @Test
  @DisplayName("조회 → 다른 트랜잭션 수정 → 오래된 버전 저장 시 낙관적 잠금 충돌로 ProjectVersionConflictException이 발생한다")
  void readUpdateThenSaveOldVersion() {
    // 트랜잭션 1: 프로젝트 조회 (version 0)
    Project stale = tx.execute(status -> repository.findById(projectId).orElseThrow());
    assertThat(stale.getVersion()).isZero();

    // 트랜잭션 2: 같은 프로젝트를 다시 읽어 수정 (version 1)
    Project updated = repository.save(withTitle(
        tx.execute(status -> repository.findById(projectId).orElseThrow()), "먼저 수정"));
    assertThat(updated.getVersion()).isEqualTo(1L);

    // 트랜잭션 3: version 0 인 오래된 객체로 저장 시도 → merge 시 버전 충돌
    Project oldProject = withTitle(stale, "나중에 수정");
    assertThatThrownBy(() -> repository.save(oldProject))
        .isInstanceOf(ProjectVersionConflictException.class);

    ProjectJpaEntity current = tx.execute(status ->
        entityManager.find(ProjectJpaEntity.class, projectId));
    assertThat(current.getTitle()).isEqualTo("먼저 수정");
    assertThat(current.getVersion()).isEqualTo(1L);
  }

  private Project withTitle(Project source, String title) {
    return Project.builder()
        .id(source.getId())
        .teamId(source.getTeamId())
        .title(title)
        .description(source.getDescription())
        .goal(source.getGoal())
        .repositoryUrl(source.getRepositoryUrl())
        .externalLinks(source.getExternalLinks())
        .approvalStatus(source.getApprovalStatus())
        .meetingStyle(source.getMeetingStyle())
        .createdAt(source.getCreatedAt())
        .version(source.getVersion())
        .build();
  }

  private ObjectNode externalLinks() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("notion", "https://notion.so/example");
    return node;
  }
}
