package kgu.developers.api.project.application;

import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
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

    private void validateTeamMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 프로젝트 제안서에 접근할 수 있습니다.");
        }
    }
}
