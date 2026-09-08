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
    Long topicCandidateId,
    String title,
    String description,
    String goal,
    JsonNode dataConfiguration,
    JsonNode screenConfiguration,
    String collaborationStyle,
    String projectSchedule,
    String repositoryUrl,
    JsonNode externalLinks,
    ApprovalStatus approvalStatus,
    LocalDateTime proposalCompletedAt
) {
    // screenConfiguration은 호출부(ProjectFacade)가 imageFileId를 presigned URL로 보강해 넘겨준 걸
    // 그대로 쓴다 — 원본 project.getScreenConfiguration()을 여기서 직접 읽지 않는 이유는, 그러면
    // 이 메서드를 새로 쓰는 곳마다 보강을 깜빡하고 imageFileId만 내려주는 실수가 반복될 수 있기
    // 때문이다(발표자료 screens가 실제로 그렇게 한 번 새어나갔다, PresentationContentResponse 참고).
    public static ProjectResponse from(Project project, JsonNode resolvedScreenConfiguration) {
        return ProjectResponse.builder()
            .id(project.getId())
            .teamId(project.getTeamId())
            .topicCandidateId(project.getTopicCandidateId())
            .title(project.getTitle())
            .description(project.getDescription())
            .goal(project.getGoal())
            .dataConfiguration(project.getDataConfiguration())
            .screenConfiguration(resolvedScreenConfiguration)
            .collaborationStyle(project.getCollaborationStyle())
            .projectSchedule(project.getProjectSchedule())
            .repositoryUrl(project.getRepositoryUrl())
            .externalLinks(project.getExternalLinks())
            .approvalStatus(project.getApprovalStatus())
            .proposalCompletedAt(project.getProposalCompletedAt())
            .build();
    }
}
