package kgu.developers.domain.teamthread.application.command;

import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamThreadCommandService {

    private final TeamThreadRepository teamThreadRepository;

    public TeamThread getOrCreateThread(Long teamId) {
        return teamThreadRepository.findByTeamId(teamId)
            .orElseGet(() -> teamThreadRepository.save(TeamThread.create(teamId)));
    }
}
