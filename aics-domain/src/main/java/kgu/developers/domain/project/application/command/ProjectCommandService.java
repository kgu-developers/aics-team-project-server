package kgu.developers.domain.project.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.project.exception.ProjectApprovalRequiredException;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

    private static final JsonNode EMPTY_ARRAY_NODE = JsonNodeFactory.instance.arrayNode();

    private final ProjectRepository projectRepository;
    private final ProjectApprovalRepository projectApprovalRepository;
    private final TeamMemberRepository teamMemberRepository;

    public Project saveProject(
        Long teamId,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks,
        String dataConfiguration,
        JsonNode screenConfiguration,
        JsonNode keyFeatures,
        JsonNode demoFlow
    ) {
        return saveProject(teamId, title, description, goal, meetingStyle, repositoryUrl, externalLinks, null, dataConfiguration, screenConfiguration, keyFeatures, sortDemoFlow(demoFlow));
    }

    public Project saveProject(
        Long teamId,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks,
        Long topicCandidateId,
        String dataConfiguration,
        JsonNode screenConfiguration,
        JsonNode keyFeatures,
        JsonNode demoFlow
    ) {
        // 팀당 프로젝트는 하나다. 조회-수정-저장이 갈라지지 않도록 팀 행을 먼저 잠근다.
        projectRepository.lockTeam(teamId);

        JsonNode sortedDemoFlow = sortDemoFlow(demoFlow);

        Project existing = projectRepository.findIncludingDeletedByTeamId(teamId).orElse(null);
        if (existing == null) {
            return projectRepository.save(Project.create(
                teamId, title, description, goal, repositoryUrl, externalLinks, ApprovalStatus.DRAFT, meetingStyle, topicCandidateId, dataConfiguration, screenConfiguration, keyFeatures, sortedDemoFlow
            ));
        }
        if (existing.getDeletedAt() != null) {
            return reactivateProject(existing, title, description, goal, meetingStyle, repositoryUrl, externalLinks, topicCandidateId, dataConfiguration, screenConfiguration, keyFeatures, sortedDemoFlow);
        }
        return updateProject(existing, title, description, goal, meetingStyle, repositoryUrl, externalLinks, topicCandidateId, dataConfiguration, screenConfiguration, keyFeatures, sortedDemoFlow);
    }

    public Project finalizeTopic(Long teamId, Long topicCandidateId, String title, String description, String goal) {
        projectRepository.lockTeam(teamId);

        Project active = projectRepository.findIncludingDeletedByTeamId(teamId)
            .filter(project -> project.getDeletedAt() == null)
            .orElse(null);

        Project project = saveProject(
            teamId,
            title,
            description,
            goal,
            active == null ? null : active.getMeetingStyle(),
            active == null ? null : active.getRepositoryUrl(),
            active == null ? null : active.getExternalLinks(),
            topicCandidateId,
            // 주제 확정은 제안서 내용을 입력받지 않는다. 기존 제안서가 있으면 그대로 옮기고,
            // 없으면(최초 확정이라 프로젝트를 새로 만드는 경우) NOT NULL이라 빈 값으로 채운다.
            active == null ? "" : active.getDataConfiguration(),
            active == null ? EMPTY_ARRAY_NODE : active.getScreenConfiguration(),
            active == null ? EMPTY_ARRAY_NODE : active.getKeyFeatures(),
            sortDemoFlow(active == null ? EMPTY_ARRAY_NODE : active.getDemoFlow())
        );

        return project;
    }

    /**
     * 소프트 삭제된 프로젝트를 새 제안서로 되살린다.
     * 되살린 제안서는 새 리비전이고, 이전 리비전의 동의는 모두 무효가 된다.
     */
    private Project reactivateProject(
        Project project,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks,
        Long topicCandidateId,
        String dataConfiguration,
        JsonNode screenConfiguration,
        JsonNode keyFeatures,
        JsonNode demoFlow
    ) {
        projectApprovalRepository.deleteAllByProjectId(project.getId());
        return projectRepository.reactivate(project.getId(), Project.create(
            project.getTeamId(), title, description, goal, repositoryUrl, externalLinks, ApprovalStatus.DRAFT, meetingStyle, topicCandidateId, dataConfiguration, screenConfiguration, keyFeatures, demoFlow
        ));
    }

    private Project updateProject(
        Project project,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks,
        Long topicCandidateId,
        String dataConfiguration,
        JsonNode screenConfiguration,
        JsonNode keyFeatures,
        JsonNode demoFlow
    ) {
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }

        if (project.hasSameProposalContent(title, description, goal, meetingStyle, repositoryUrl, externalLinks, dataConfiguration, screenConfiguration, keyFeatures, demoFlow) &&
            (topicCandidateId == null || topicCandidateId.equals(project.getTopicCandidateId()))) {
            return project;
        }

        project.updateTitle(title);
        project.updateDescription(description);
        project.updateGoal(goal);
        project.updateDataConfiguration(dataConfiguration);
        project.updateScreenConfiguration(screenConfiguration);
        project.updateKeyFeatures(keyFeatures);
        project.updateDemoFlow(demoFlow);
        project.updateMeetingStyle(meetingStyle);
        project.updateRepositoryUrl(repositoryUrl);
        project.updateExternalLinks(externalLinks);
        if (topicCandidateId != null) {
            project.updateTopicCandidateId(topicCandidateId);
        }
        project.updateApprovalStatus(ApprovalStatus.DRAFT);
        project.increaseProposalRevision();
        projectApprovalRepository.deleteAllByProjectId(project.getId());

        return projectRepository.save(project);
    }

    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
        projectApprovalRepository.deleteAllByProjectId(projectId);
    }

    public void lockTeam(Long teamId) {
        projectRepository.lockTeam(teamId);
    }

    private JsonNode sortDemoFlow(JsonNode demoFlow) {
        if (demoFlow == null || !demoFlow.isArray()) {
            return EMPTY_ARRAY_NODE;
        }
        
        ArrayNode sorted = JsonNodeFactory.instance.arrayNode();
        List<JsonNode> items = new ArrayList<>();
        demoFlow.forEach(items::add);
        
        items.sort(Comparator.comparing(node -> {
            JsonNode number = node.get("number");
            if (number == null || number.isNull()) {
                return Integer.MAX_VALUE;
            }
            return number.asInt();
        }));
        
        items.forEach(sorted::add);
        return sorted;
    }

    public void completeProposal(Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(ProjectNotFoundException::new);
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        List<TeamMember> members = teamMemberRepository.findAllByTeamId(project.getTeamId());
        Set<String> approvedUserIds = projectApprovalRepository
            .findAllByProjectIdAndProposalRevision(projectId, project.getProposalRevision()).stream()
            .map(ProjectApproval::getUserId)
            .collect(toSet());
        boolean allMembersApproved = !members.isEmpty() && members.stream()
            .allMatch(member -> approvedUserIds.contains(member.getUserId()));
        if (!allMembersApproved) {
            throw new ProjectApprovalRequiredException();
        }
        project.completeProposal();
        projectRepository.save(project);
    }
}
