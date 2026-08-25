package kgu.developers.api.project.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProjectResponse(
    Long id,
    Long teamId,
    String title,
    String description,
    String goal,
    String meetingStyle,
    String repositoryUrl,
    JsonNode externalLinks,
    ApprovalStatus approvalStatus,
    LocalDateTime proposalCompletedAt
) {
    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
            .id(project.getId())
            .teamId(project.getTeamId())
            .title(project.getTitle())
            .description(project.getDescription())
            .goal(project.getGoal())
            .meetingStyle(project.getMeetingStyle())
            .repositoryUrl(project.getRepositoryUrl())
            .externalLinks(project.getExternalLinks())
            .approvalStatus(project.getApprovalStatus())
            .proposalCompletedAt(project.getProposalCompletedAt())
            .build();
    }
}
