package kgu.developers.domain.teamthread.application.query;

import java.util.List;
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

    public TeamThread getThreadById(Long id) {
        return teamThreadRepository.findById(id)
            .orElseThrow(TeamThreadNotFoundException::new);
    }

    public List<TeamThread> getThreads(List<Long> teamIds) {
        return teamThreadRepository.findAllByTeamIdIn(teamIds);
    }
}
