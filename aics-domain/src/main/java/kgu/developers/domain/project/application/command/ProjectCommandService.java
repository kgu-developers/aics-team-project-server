package kgu.developers.domain.project.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectApprovalRepository projectApprovalRepository;

    public Project saveProject(
        Long teamId,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks
    ) {
        return projectRepository.findAllByTeamId(teamId).stream()
            .findFirst()
            .map(project -> updateProject(project, title, description, goal, meetingStyle, repositoryUrl, externalLinks))
            .orElseGet(() -> projectRepository.save(Project.create(
                teamId, title, description, goal, repositoryUrl, externalLinks, ApprovalStatus.DRAFT, meetingStyle
            )));
    }

    private Project updateProject(
        Project project,
        String title,
        String description,
        String goal,
        String meetingStyle,
        String repositoryUrl,
        JsonNode externalLinks
    ) {
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }

        project.updateTitle(title);
        project.updateDescription(description);
        project.updateGoal(goal);
        project.updateMeetingStyle(meetingStyle);
        project.updateRepositoryUrl(repositoryUrl);
        project.updateExternalLinks(externalLinks);
        project.updateApprovalStatus(ApprovalStatus.DRAFT);

        projectApprovalRepository.findAllByProjectId(project.getId())
            .forEach(approval -> projectApprovalRepository.deleteById(approval.getId()));

        return projectRepository.save(project);
    }

    public void completeProposal(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new);
        project.completeProposal();
        projectRepository.save(project);
    }
}
