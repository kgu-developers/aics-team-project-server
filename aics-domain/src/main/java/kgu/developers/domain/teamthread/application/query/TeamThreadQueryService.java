package kgu.developers.domain.teamthread.application.query;

import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeamThreadQueryService {

    private final TeamThreadRepository teamThreadRepository;

    public TeamThread getThread(Long teamId) {
        return teamThreadRepository.findByTeamId(teamId)
            .orElseThrow(TeamThreadNotFoundException::new);
    }
}
