package kgu.developers.domain.project.application.command;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

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
        JsonNode externalLinks
    ) {
        projectRepository.lockTeam(teamId);

        return projectRepository.findAllByTeamIdIncludingDeletedForUpdate(teamId).stream()
            .findFirst()
            .map(project -> project.getDeletedAt() == null
                ? updateProject(project, title, description, goal, meetingStyle, repositoryUrl, externalLinks, false)
                : restoreProject(project, title, description, goal, meetingStyle, repositoryUrl, externalLinks))
            .orElseGet(() -> projectRepository.save(Project.create(
                teamId, title, description, goal, repositoryUrl, externalLinks, ApprovalStatus.DRAFT, meetingStyle
            )));
    }

    private Project restoreProject(
        Project project,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks
    ) {
        project.restore();
        return updateProject(project, title, description, goal, meetingStyle, repositoryUrl, externalLinks, true);
    }

    private Project updateProject(
        Project project,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks,
        boolean forceApprovalReset
    ) {
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }

        if (!forceApprovalReset && project.hasSameProposalContent(title, description, goal, meetingStyle, repositoryUrl, externalLinks)) {
            return project;
        }

        project.updateTitle(title);
        project.updateDescription(description);
        project.updateGoal(goal);
        project.updateMeetingStyle(meetingStyle);
        project.updateRepositoryUrl(repositoryUrl);
        project.updateExternalLinks(externalLinks);
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
