package kgu.developers.api.teamthread.application;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.teamthread.presentation.response.TeamThreadResponse;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class TeamThreadFacade {

    private final TeamThreadCommandService teamThreadCommandService;
    private final TeamAccessValidator teamAccessValidator;

    public TeamThreadResponse getOrCreateThread(Long teamId, String userId) {
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        TeamThread teamThread = teamThreadCommandService.getOrCreateThread(teamId);
        return TeamThreadResponse.from(teamThread);
    }
}
