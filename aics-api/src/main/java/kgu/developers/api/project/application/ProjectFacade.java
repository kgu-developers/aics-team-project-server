package kgu.developers.api.project.application;

import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.project.presentation.response.ProjectApprovalSummaryResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ProjectFacade {

    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;
    private final TeamAccessValidator teamAccessValidator;
    private final ProjectApprovalRepository projectApprovalRepository;
    private final ProjectApprovalCommandService projectApprovalCommandService;

    public ProjectResponse getProject(Long teamId, String userId) {
        teamAccessValidator.validateMembership(teamId, userId);
        return ProjectResponse.from(projectQueryService.getProjectByTeamId(teamId));
    }

    public ProjectResponse saveProject(Long teamId, String userId, ProjectRequest request) {
        teamAccessValidator.validateMembership(teamId, userId);
        return ProjectResponse.from(projectCommandService.saveProject(
            teamId,
            request.title(),
            request.description(),
            request.goal(),
            request.meetingStyle(),
            request.repositoryUrl(),
            request.externalLinks()
        ));
    }

    public void completeProposal(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        
        projectCommandService.lockTeam(project.getTeamId());
        teamAccessValidator.validateTeamLeader(project.getTeamId(), userId);

        projectCommandService.completeProposal(projectId);
    }

    public void deleteProject(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateTeamLeader(project.getTeamId(), userId);

        projectCommandService.deleteProject(projectId);
    }

    public void approveProject(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);
        projectApprovalCommandService.approve(projectId, userId);
    }

    public ProjectApprovalSummaryResponse getApprovalSummary(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);
        ApprovalCount count = projectApprovalRepository.countApprovalsByTeamMembers(
            projectId, project.getTeamId(), project.getProposalRevision()
        );
        return ProjectApprovalSummaryResponse.of((int) count.approvedMembers(), (int) count.totalMembers());
    }
}
