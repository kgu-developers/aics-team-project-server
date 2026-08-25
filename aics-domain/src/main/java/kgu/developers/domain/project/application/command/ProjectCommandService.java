package kgu.developers.domain.project.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        projectApprovalRepository.findAllByProjectId(project.getId())
            .forEach(approval -> projectApprovalRepository.deleteById(approval.getId()));

        return projectRepository.save(project);
    }

    public void completeProposal(Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(ProjectNotFoundException::new);
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        boolean allMembersApproved = teamMemberRepository.findAllByTeamId(project.getTeamId()).stream()
            .allMatch(member -> projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(
                projectId, member.getUserId(), project.getProposalRevision()
            ));
        if (!allMembersApproved) {
            throw new kgu.developers.domain.project.exception.ProjectApprovalRequiredException();
        }
        project.completeProposal();
        projectRepository.save(project);
    }
}
