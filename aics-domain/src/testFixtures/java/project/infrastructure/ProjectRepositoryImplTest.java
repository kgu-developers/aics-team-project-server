package project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.infrastructure.JpaProjectRepository;
import kgu.developers.domain.project.infrastructure.ProjectJpaEntity;
import kgu.developers.domain.project.infrastructure.ProjectRepositoryImpl;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;

@ExtendWith(MockitoExtension.class)
class ProjectRepositoryImplTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private JpaProjectRepository jpaProjectRepository;

  @Mock
  private EntityManager entityManager;

  @Test
  @DisplayName("save는 저장 결과를 도메인으로 반환한다")
  void save() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project project = Project.create(
        1L,
        "팀 프로젝트",
        "프로젝트 설명",
        "프로젝트 목표",
        "https://github.com/example/repo",
        externalLinks,
        ApprovalStatus.DRAFT,
        "온라인"
    );

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.getReference(TeamJpaEntity.class, 1L)).willReturn(team);

    Project savedProject = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("팀 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .build();

    given(jpaProjectRepository.save(any(ProjectJpaEntity.class)))
        .willReturn(ProjectJpaEntity.toEntity(savedProject, team));

    Project result = repository.save(project);

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getTitle()).isEqualTo("팀 프로젝트");
    ArgumentCaptor<ProjectJpaEntity> captor = ArgumentCaptor.forClass(ProjectJpaEntity.class);
    verify(jpaProjectRepository).save(captor.capture());
    assertThat(captor.getValue().getTeam().getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("findById는 id로 삭제되지 않은 프로젝트를 조회한다")
  void findById() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project project = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("팀 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(jpaProjectRepository.findByIdAndDeletedAtIsNull(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(project, team)));

    Optional<Project> found = repository.findById(1L);

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("팀 프로젝트");
    assertThat(found.get().getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
  }

  @Test
  @DisplayName("findById는 존재하지 않는 id면 빈 Optional을 반환한다")
  void findByIdNotFound() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    given(jpaProjectRepository.findByIdAndDeletedAtIsNull(999L))
        .willReturn(Optional.empty());

    Optional<Project> found = repository.findById(999L);

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("findAllById는 id 목록으로 삭제되지 않은 프로젝트들을 조회한다")
  void findAllById() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks1 = objectMapper.createObjectNode();
    externalLinks1.put("notion", "https://notion.so/example1");

    ObjectNode externalLinks2 = objectMapper.createObjectNode();
    externalLinks2.put("notion", "https://notion.so/example2");

    Project project1 = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("프로젝트1")
        .description("설명1")
        .goal("목표1")
        .repositoryUrl("https://github.com/example/repo1")
        .externalLinks(externalLinks1)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .build();

    Project project2 = Project.builder()
        .id(2L)
        .teamId(1L)
        .title("프로젝트2")
        .description("설명2")
        .goal("목표2")
        .repositoryUrl("https://github.com/example/repo2")
        .externalLinks(externalLinks2)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("오프라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();

    given(jpaProjectRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
        .willReturn(List.of(
            ProjectJpaEntity.toEntity(project1, team),
            ProjectJpaEntity.toEntity(project2, team)
        ));

    List<Project> projects = repository.findAllById(List.of(1L, 2L));

    assertThat(projects).hasSize(2);
    assertThat(projects).extracting(Project::getTitle).containsExactly("프로젝트1", "프로젝트2");
  }

  @Test
  @DisplayName("findAllByTeamId는 팀 id로 삭제되지 않은 프로젝트들을 조회한다")
  void findAllByTeamId() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks1 = objectMapper.createObjectNode();
    externalLinks1.put("notion", "https://notion.so/example1");

    ObjectNode externalLinks2 = objectMapper.createObjectNode();
    externalLinks2.put("notion", "https://notion.so/example2");

    Project project1 = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("프로젝트1")
        .description("설명1")
        .goal("목표1")
        .repositoryUrl("https://github.com/example/repo1")
        .externalLinks(externalLinks1)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .build();

    Project project2 = Project.builder()
        .id(2L)
        .teamId(1L)
        .title("프로젝트2")
        .description("설명2")
        .goal("목표2")
        .repositoryUrl("https://github.com/example/repo2")
        .externalLinks(externalLinks2)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("오프라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();

    given(jpaProjectRepository.findAllByTeamIdAndDeletedAtIsNull(1L))
        .willReturn(List.of(
            ProjectJpaEntity.toEntity(project1, team),
            ProjectJpaEntity.toEntity(project2, team)
        ));

    List<Project> projects = repository.findAllByTeamId(1L);

    assertThat(projects).hasSize(2);
    assertThat(projects).extracting(Project::getTitle).containsExactly("프로젝트1", "프로젝트2");
  }

  @Test
  @DisplayName("deleteById는 id로 프로젝트를 삭제한다")
  void deleteById() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project project = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("팀 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    ProjectJpaEntity entity = ProjectJpaEntity.toEntity(project, team);

    given(jpaProjectRepository.findByIdAndDeletedAtIsNull(1L))
        .willReturn(Optional.of(entity));

    repository.deleteById(1L);

    assertThat(entity.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("deleteById는 존재하지 않는 id면 예외를 발생시킨다")
  void deleteByIdNotFound() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    given(jpaProjectRepository.findByIdAndDeletedAtIsNull(999L))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> repository.deleteById(999L))
        .isInstanceOf(ProjectNotFoundException.class);
  }
}
