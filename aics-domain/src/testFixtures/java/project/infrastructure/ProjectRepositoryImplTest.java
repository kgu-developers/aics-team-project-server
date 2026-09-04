package project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.time.LocalDateTime;
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
import kgu.developers.domain.project.exception.ProjectAlreadyExistsException;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.exception.ProjectVersionConflictException;
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
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

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
        .version(0L)
        .build();

    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willReturn(ProjectJpaEntity.toEntity(savedProject, team));

    Project result = repository.save(project);

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getTitle()).isEqualTo("팀 프로젝트");
    ArgumentCaptor<ProjectJpaEntity> captor = ArgumentCaptor.forClass(ProjectJpaEntity.class);
    verify(jpaProjectRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getTeam().getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("save는 존재하지 않는 팀이면 TeamNotFoundException을 발생시킨다")
  void saveWithNonExistentTeam() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project project = Project.create(
        999L,
        "팀 프로젝트",
        "프로젝트 설명",
        "프로젝트 목표",
        "https://github.com/example/repo",
        externalLinks,
        ApprovalStatus.DRAFT,
        "온라인"
    );

    given(entityManager.find(TeamJpaEntity.class, 999L, PESSIMISTIC_WRITE)).willReturn(null);

    assertThatThrownBy(() -> repository.save(project))
        .isInstanceOf(kgu.developers.domain.team.exception.TeamNotFoundException.class);
  }

  @Test
  @DisplayName("save는 소프트 삭제된 팀이면 TeamNotFoundException을 발생시킨다")
  void saveWithDeletedTeam() {
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

    TeamJpaEntity deletedTeam = TeamJpaEntity.builder().id(1L).build();
    deletedTeam.delete();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(deletedTeam);

    assertThatThrownBy(() -> repository.save(project))
        .isInstanceOf(kgu.developers.domain.team.exception.TeamNotFoundException.class);
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("save는 팀에 활성 프로젝트가 이미 있으면 ProjectAlreadyExistsException을 발생시킨다")
  void saveWithExistingActiveProject() {
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
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    Project existing = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("기존 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(existing, team)));

    assertThatThrownBy(() -> repository.save(project))
        .isInstanceOf(ProjectAlreadyExistsException.class);
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("save는 팀에 소프트 삭제된 프로젝트가 있고 ID가 일치해도 ProjectVersionConflictException을 발생시킨다")
  void saveWithSoftDeletedProjectAndMatchingIdThrowsVersionConflict() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project newProject = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("새 프로젝트")
        .description("새 설명")
        .goal("새 목표")
        .repositoryUrl("https://github.com/example/new-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    Project deletedProject = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("삭제된 프로젝트")
        .description("설명")
        .goal("목표")
        .repositoryUrl("https://github.com/example/old-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("오프라인")
        .deletedAt(LocalDateTime.now())
        .build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(deletedProject, team)));

    assertThatThrownBy(() -> repository.save(newProject))
        .isInstanceOf(ProjectVersionConflictException.class);
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("save는 팀에 소프트 삭제된 프로젝트가 있고 ID가 다르면 ProjectVersionConflictException을 발생시킨다")
  void saveWithSoftDeletedProjectAndDifferentIdThrowsVersionConflict() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project newProject = Project.builder()
        .id(null)  // ID가 null인 경우
        .teamId(1L)
        .title("새 프로젝트")
        .description("새 설명")
        .goal("새 목표")
        .repositoryUrl("https://github.com/example/new-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    Project deletedProject = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("삭제된 프로젝트")
        .description("설명")
        .goal("목표")
        .repositoryUrl("https://github.com/example/old-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("오프라인")
        .deletedAt(LocalDateTime.now())
        .build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(deletedProject, team)));

    assertThatThrownBy(() -> repository.save(newProject))
        .isInstanceOf(ProjectVersionConflictException.class);
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("reactivate는 삭제된 프로젝트를 재활성화한다")
  void reactivateRestoresSoftDeletedProject() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project newProject = Project.builder()
        .teamId(1L)
        .title("새 프로젝트")
        .description("새 설명")
        .goal("새 목표")
        .repositoryUrl("https://github.com/example/new-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    Project deletedProject = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("삭제된 프로젝트")
        .description("설명")
        .goal("목표")
        .repositoryUrl("https://github.com/example/old-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("오프라인")
        .deletedAt(LocalDateTime.now())
        .build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(deletedProject, team)));

    Project reactivated = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("새 프로젝트")
        .description("새 설명")
        .goal("새 목표")
        .repositoryUrl("https://github.com/example/new-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .deletedAt(null)
        .proposalCompletedAt(null)
        .build();
    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willReturn(ProjectJpaEntity.toEntity(reactivated, team));

    Project result = repository.reactivate(1L, newProject);

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getTitle()).isEqualTo("새 프로젝트");
    assertThat(result.getDeletedAt()).isNull();
    assertThat(result.getProposalCompletedAt()).isNull();
  }

  @Test
  @DisplayName("save는 복구된 프로젝트라도 팀에 다른 활성 프로젝트가 있으면 ProjectAlreadyExistsException을 발생시킨다")
  void saveReactivatedProjectWithAnotherActiveProject() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();

    Project reactivated = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("복구된 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    Project other = Project.builder()
        .id(2L)
        .teamId(1L)
        .title("다른 활성 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(other, team)));

    assertThatThrownBy(() -> repository.save(reactivated))
        .isInstanceOf(ProjectAlreadyExistsException.class);
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("reactivate는 존재하지 않는 프로젝트 ID로 호출하면 ProjectNotFoundException을 발생시킨다")
  void reactivateWithNonExistentIdThrowsException() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project newProject = Project.builder()
        .teamId(1L)
        .title("새 프로젝트")
        .description("새 설명")
        .goal("새 목표")
        .repositoryUrl("https://github.com/example/new-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> repository.reactivate(999L, newProject))
        .isInstanceOf(ProjectNotFoundException.class);
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("reactivate는 이미 활성화된 프로젝트에 대해 호출하면 IllegalStateException을 발생시킨다")
  void reactivateActiveProjectThrowsException() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project newProject = Project.builder()
        .teamId(1L)
        .title("새 프로젝트")
        .description("새 설명")
        .goal("새 목표")
        .repositoryUrl("https://github.com/example/new-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    Project activeProject = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("활성 프로젝트")
        .description("설명")
        .goal("목표")
        .repositoryUrl("https://github.com/example/old-repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("오프라인")
        .deletedAt(null)
        .build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(activeProject, team)));

    assertThatThrownBy(() -> repository.reactivate(1L, newProject))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("이미 활성화된 프로젝트는 재활성화할 수 없습니다.");
    verify(jpaProjectRepository, never()).saveAndFlush(any(ProjectJpaEntity.class));
  }

  @Test
  @DisplayName("save는 활성 프로젝트를 수정할 때 자기 자신을 중복으로 보지 않는다")
  void saveExistingActiveProjectDoesNotConflictWithItself() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();

    Project project = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("수정된 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(project, team)));
    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willReturn(ProjectJpaEntity.toEntity(project, team));

    Project result = repository.save(project);

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getTitle()).isEqualTo("수정된 프로젝트");
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
        .version(0L)
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

    given(jpaProjectRepository.findAllByIdInAndDeletedAtIsNullOrderByIdAsc(List.of(1L, 2L)))
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
        .version(0L)
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

  @Test
  @DisplayName("findIncludingDeletedByTeamId는 팀 id로 삭제된 프로젝트를 포함하여 조회한다")
  void findIncludingDeletedByTeamId() {
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
        .deletedAt(LocalDateTime.now())
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(jpaProjectRepository.findByTeamId(1L))
        .willReturn(Optional.of(ProjectJpaEntity.toEntity(project, team)));

    Optional<Project> found = repository.findIncludingDeletedByTeamId(1L);

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("팀 프로젝트");
    assertThat(found.get().getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("findIncludingDeletedByTeamId는 존재하지 않는 팀 id면 빈 Optional을 반환한다")
  void findIncludingDeletedByTeamIdNotFound() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    given(jpaProjectRepository.findByTeamId(999L))
        .willReturn(Optional.empty());

    Optional<Project> found = repository.findIncludingDeletedByTeamId(999L);

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("save는 낙관적 잠금 충돌 시 ProjectVersionConflictException을 발생시킨다")
  void saveOptimisticLockingFailure() {
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
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .version(1L)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
            ProjectJpaEntity.class, 1L));

    assertThatThrownBy(() -> repository.save(project))
        .isInstanceOf(kgu.developers.domain.project.exception.ProjectVersionConflictException.class);
  }

  @Test
  @DisplayName("삭제 대 수정 트랜잭션: 이미 삭제된 프로젝트를 수정하려 하면 충돌 예외가 발생한다")
  void updateAfterDeleteTransaction() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    // 트랜잭션 1: 프로젝트 삭제
    Project projectToDelete = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("팀 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .version(1L)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);
    ProjectJpaEntity entityToDelete = ProjectJpaEntity.toEntity(projectToDelete, team);

    given(jpaProjectRepository.findByIdAndDeletedAtIsNull(1L))
        .willReturn(Optional.of(entityToDelete));

    repository.deleteById(1L);

    // 트랜잭션 2: 삭제 전에 읽은 객체로 수정 시도 (낙관적 잠금 충돌)
    Project projectToUpdate = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("수정된 제목")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .version(1L) // 이전 버전으로 시도
        .build();

    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
            ProjectJpaEntity.class, 1L));

    assertThatThrownBy(() -> repository.save(projectToUpdate))
        .isInstanceOf(kgu.developers.domain.project.exception.ProjectVersionConflictException.class);
  }

  @Test
  @DisplayName("수정 대 수정 트랜잭션: 동시에 수정 시도 시 충돌 예외가 발생한다")
  void updateAfterUpdateTransaction() {
    ProjectRepositoryImpl repository = new ProjectRepositoryImpl(jpaProjectRepository, entityManager);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    // 트랜잭션 1: 첫 번째 수정
    Project projectFirstUpdate = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("첫 번째 수정")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .version(1L)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(1L).build();
    given(entityManager.find(TeamJpaEntity.class, 1L, PESSIMISTIC_WRITE)).willReturn(team);

    ProjectJpaEntity updatedEntity = ProjectJpaEntity.builder()
        .id(1L)
        .team(team)
        .title("첫 번째 수정")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .version(2L) // 버전 증가
        .build();

    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willReturn(updatedEntity);

    Project result = repository.save(projectFirstUpdate);
    assertThat(result.getVersion()).isEqualTo(2L);

    // 트랜잭션 2: 이전 버전으로 두 번째 수정 시도 (낙관적 잠금 충돌)
    Project projectSecondUpdate = Project.builder()
        .id(1L)
        .teamId(1L)
        .title("두 번째 수정")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .version(1L) // 이전 버전으로 시도
        .build();

    given(jpaProjectRepository.saveAndFlush(any(ProjectJpaEntity.class)))
        .willThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
            ProjectJpaEntity.class, 1L));

    assertThatThrownBy(() -> repository.save(projectSecondUpdate))
        .isInstanceOf(kgu.developers.domain.project.exception.ProjectVersionConflictException.class);
  }
}
