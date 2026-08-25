package kgu.developers.api.project.application;

import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectApprovalRequiredException;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ProjectFacade {

    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectApprovalRepository projectApprovalRepository;
    private final ProjectApprovalCommandService projectApprovalCommandService;

    public ProjectResponse getProject(Long teamId, String userId) {
        validateTeamMembership(teamId, userId);
        return ProjectResponse.from(projectQueryService.getProjectByTeamId(teamId));
    }

    public ProjectResponse saveProject(Long teamId, String userId, ProjectRequest request) {
        validateTeamMembership(teamId, userId);
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
        validateTeamLeader(project.getTeamId(), userId);

        boolean allMembersApproved = teamMemberRepository.findAllByTeamId(project.getTeamId()).stream()
            .allMatch(member -> projectApprovalRepository.existsByProjectIdAndUserId(projectId, member.getUserId()));
        if (!allMembersApproved) {
            throw new ProjectApprovalRequiredException();
        }

        projectCommandService.completeProposal(projectId);
    }

    public void approveProject(Long projectId, String userId) {
        Project project = projectQueryService.getProject(projectId);
        validateTeamMembership(project.getTeamId(), userId);
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        projectApprovalCommandService.approve(projectId, userId);
    }

    private void validateTeamMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 프로젝트 제안서에 접근할 수 있습니다.");
        }
    }

    private void validateTeamLeader(Long teamId, String userId) {
        boolean isLeader = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            .map(member -> member.isLeader())
            .orElse(false);
        if (!isLeader) {
            throw new AccessDeniedException("팀장만 프로젝트 제안 단계를 완료할 수 있습니다.");
        }
    }
}
