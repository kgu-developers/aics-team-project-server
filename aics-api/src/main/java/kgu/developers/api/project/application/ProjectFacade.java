package kgu.developers.api.project.application;

import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.project.presentation.response.ProjectApprovalSummaryResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional
public class ProjectFacade {

    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;
    private final TeamAccessValidator teamAccessValidator;
    private final TeamMemberRepository teamMemberRepository;
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
        teamAccessValidator.validateTeamLeader(project.getTeamId(), userId);

        projectCommandService.completeProposal(projectId);
    }

    public void approveProject(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);
        projectApprovalCommandService.approve(projectId, userId);
    }

    public ProjectApprovalSummaryResponse getApprovalSummary(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        teamAccessValidator.validateMembership(project.getTeamId(), userId);
        Set<String> activeMemberIds = teamMemberRepository.findAllByTeamId(project.getTeamId()).stream()
            .map(member -> member.getUserId())
            .collect(java.util.stream.Collectors.toSet());
        int totalCount = activeMemberIds.size();
        int approvedCount = (int) projectApprovalRepository
            .findAllByProjectIdAndProposalRevision(projectId, project.getProposalRevision()).stream()
            .filter(approval -> activeMemberIds.contains(approval.getUserId()))
            .count();
        return ProjectApprovalSummaryResponse.of(approvedCount, totalCount);
    }
}
