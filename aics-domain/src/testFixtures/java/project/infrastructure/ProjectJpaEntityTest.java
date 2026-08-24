package project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.infrastructure.ProjectJpaEntity;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;

class ProjectJpaEntityTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("toEntity는 기존 프로젝트의 생성일과 삭제일을 그대로 옮긴다")
  void toEntityKeepsTimestamps() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project project = Project.builder()
        .id(1L)
        .teamId(2L)
        .title("팀 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.DRAFT)
        .meetingStyle("온라인")
        .proposalCompletedAt(LocalDateTime.of(2026, 2, 1, 12, 0))
        .createdAt(createdAt)
        .deletedAt(deletedAt)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(2L).build();
    ProjectJpaEntity entity = ProjectJpaEntity.toEntity(project, team);

    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getUpdatedAt()).isNull();
    assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
  }

  @Test
  @DisplayName("toDomain는 엔티티의 모든 필드를 도메인으로 변환한다")
  void toDomainConvertsAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
    LocalDateTime proposalCompletedAt = LocalDateTime.of(2026, 2, 1, 12, 0);

    ObjectNode externalLinks = objectMapper.createObjectNode();
    externalLinks.put("notion", "https://notion.so/example");

    Project project = Project.builder()
        .id(1L)
        .teamId(2L)
        .title("팀 프로젝트")
        .description("프로젝트 설명")
        .goal("프로젝트 목표")
        .repositoryUrl("https://github.com/example/repo")
        .externalLinks(externalLinks)
        .approvalStatus(ApprovalStatus.APPROVED)
        .meetingStyle("온라인")
        .proposalCompletedAt(proposalCompletedAt)
        .createdAt(createdAt)
        .build();

    TeamJpaEntity team = TeamJpaEntity.builder().id(2L).build();
    ProjectJpaEntity entity = ProjectJpaEntity.toEntity(project, team);

    Project domain = entity.toDomain();

    assertThat(domain.getId()).isEqualTo(1L);
    assertThat(domain.getTeamId()).isEqualTo(2L);
    assertThat(domain.getTitle()).isEqualTo("팀 프로젝트");
    assertThat(domain.getDescription()).isEqualTo("프로젝트 설명");
    assertThat(domain.getGoal()).isEqualTo("프로젝트 목표");
    assertThat(domain.getRepositoryUrl()).isEqualTo("https://github.com/example/repo");
    assertThat(domain.getExternalLinks()).isEqualTo(externalLinks);
    assertThat(domain.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(domain.getMeetingStyle()).isEqualTo("온라인");
    assertThat(domain.getProposalCompletedAt()).isEqualTo(proposalCompletedAt);
    assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
    assertThat(domain.getUpdatedAt()).isNull();
  }
}
