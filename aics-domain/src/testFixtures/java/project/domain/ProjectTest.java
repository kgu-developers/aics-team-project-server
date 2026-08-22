package project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;

class ProjectTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("create는 전달받은 값으로 프로젝트를 생성한다")
  void create() {
    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");
    externalLinks.put("figma", "https://figma.com/example");

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

    assertThat(project.getTeamId()).isEqualTo(1L);
    assertThat(project.getTitle()).isEqualTo("팀 프로젝트");
    assertThat(project.getDescription()).isEqualTo("프로젝트 설명");
    assertThat(project.getGoal()).isEqualTo("프로젝트 목표");
    assertThat(project.getRepositoryUrl()).isEqualTo("https://github.com/example/repo");
    assertThat(project.getExternalLinks()).isNotNull();
    assertThat(project.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
    assertThat(project.getMeetingStyle()).isEqualTo("온라인");
    assertThat(project.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("create는 필수값이 null이면 예외를 발생시킨다")
  void createWithNullRequiredFields() {
    ObjectNode externalLinks = objectMapper.createObjectNode();

    assertThatThrownBy(() -> 
        Project.create(null, "팀 프로젝트", "설명", "목표", "repo", externalLinks, ApprovalStatus.DRAFT, "온라인")
    ).isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> 
        Project.create(1L, null, "설명", "목표", "repo", externalLinks, ApprovalStatus.DRAFT, "온라인")
    ).isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> 
        Project.create(1L, "팀 프로젝트", null, "목표", "repo", externalLinks, ApprovalStatus.DRAFT, "온라인")
    ).isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> 
        Project.create(1L, "팀 프로젝트", "설명", null, "repo", externalLinks, ApprovalStatus.DRAFT, "온라인")
    ).isInstanceOf(NullPointerException.class);

    assertThatThrownBy(() -> 
        Project.create(1L, "팀 프로젝트", "설명", "목표", "repo", externalLinks, null, "온라인")
    ).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("updateTitle은 제목을 변경한다")
  void updateTitle() {
    Project project = createDefaultProject();

    project.updateTitle("변경된 제목");

    assertThat(project.getTitle()).isEqualTo("변경된 제목");
  }

  @Test
  @DisplayName("updateTitle은 null이면 예외를 발생시킨다")
  void updateTitleWithNull() {
    Project project = createDefaultProject();

    assertThatThrownBy(() -> project.updateTitle(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("updateDescription은 설명을 변경한다")
  void updateDescription() {
    Project project = createDefaultProject();

    project.updateDescription("변경된 설명");

    assertThat(project.getDescription()).isEqualTo("변경된 설명");
  }

  @Test
  @DisplayName("updateDescription은 null이면 예외를 발생시킨다")
  void updateDescriptionWithNull() {
    Project project = createDefaultProject();

    assertThatThrownBy(() -> project.updateDescription(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("updateGoal은 목표를 변경한다")
  void updateGoal() {
    Project project = createDefaultProject();

    project.updateGoal("변경된 목표");

    assertThat(project.getGoal()).isEqualTo("변경된 목표");
  }

  @Test
  @DisplayName("updateGoal은 null이면 예외를 발생시킨다")
  void updateGoalWithNull() {
    Project project = createDefaultProject();

    assertThatThrownBy(() -> project.updateGoal(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("updateRepositoryUrl은 저장소 URL을 변경한다")
  void updateRepositoryUrl() {
    Project project = createDefaultProject();

    project.updateRepositoryUrl("https://github.com/new/repo");

    assertThat(project.getRepositoryUrl()).isEqualTo("https://github.com/new/repo");
  }

  @Test
  @DisplayName("updateRepositoryUrl은 null로 변경할 수 있다")
  void updateRepositoryUrlWithNull() {
    Project project = createDefaultProject();

    project.updateRepositoryUrl(null);

    assertThat(project.getRepositoryUrl()).isNull();
  }

  @Test
  @DisplayName("updateExternalLinks은 외부 링크를 변경한다")
  void updateExternalLinks() {
    Project project = createDefaultProject();

    ObjectNode newLinks = objectMapper.createObjectNode();
    newLinks.put("notion", "https://notion.so/new");

    project.updateExternalLinks(newLinks);

    assertThat(project.getExternalLinks()).isEqualTo(newLinks);
  }

  @Test
  @DisplayName("updateExternalLinks은 null로 변경할 수 있다")
  void updateExternalLinksWithNull() {
    Project project = createDefaultProject();

    project.updateExternalLinks(null);

    assertThat(project.getExternalLinks()).isNull();
  }

  @Test
  @DisplayName("updateApprovalStatus은 승인 상태를 변경한다")
  void updateApprovalStatus() {
    Project project = createDefaultProject();

    project.updateApprovalStatus(ApprovalStatus.PENDING);

    assertThat(project.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
  }

  @Test
  @DisplayName("updateApprovalStatus은 null이면 예외를 발생시킨다")
  void updateApprovalStatusWithNull() {
    Project project = createDefaultProject();

    assertThatThrownBy(() -> project.updateApprovalStatus(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("updateMeetingStyle은 회의 방식을 변경한다")
  void updateMeetingStyle() {
    Project project = createDefaultProject();

    project.updateMeetingStyle("오프라인");

    assertThat(project.getMeetingStyle()).isEqualTo("오프라인");
  }

  @Test
  @DisplayName("updateMeetingStyle은 null로 변경할 수 있다")
  void updateMeetingStyleWithNull() {
    Project project = createDefaultProject();

    project.updateMeetingStyle(null);

    assertThat(project.getMeetingStyle()).isNull();
  }

  @Test
  @DisplayName("completeProposal은 제안 완료 시각을 기록한다")
  void completeProposal() {
    Project project = createDefaultProject();

    assertThat(project.getProposalCompletedAt()).isNull();

    project.completeProposal();

    assertThat(project.getProposalCompletedAt()).isNotNull();
  }

  @Test
  @DisplayName("delete는 삭제 시각을 기록한다")
  void delete() {
    Project project = createDefaultProject();

    assertThat(project.getDeletedAt()).isNull();

    project.delete();

    assertThat(project.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("ApprovalStatus enum은 한글 설명을 가진다")
  void approvalStatusDescriptions() {
    assertThat(ApprovalStatus.DRAFT.getDescription()).isEqualTo("초안");
    assertThat(ApprovalStatus.PENDING.getDescription()).isEqualTo("미결정");
    assertThat(ApprovalStatus.APPROVED.getDescription()).isEqualTo("승인");
    assertThat(ApprovalStatus.REJECTED.getDescription()).isEqualTo("거절");
  }

  private Project createDefaultProject() {
    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    return Project.create(
        1L,
        "팀 프로젝트",
        "프로젝트 설명",
        "프로젝트 목표",
        "https://github.com/example/repo",
        externalLinks,
        ApprovalStatus.DRAFT,
        "온라인"
    );
  }
}
