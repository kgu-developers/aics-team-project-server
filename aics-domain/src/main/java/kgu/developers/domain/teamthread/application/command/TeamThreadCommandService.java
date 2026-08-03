package kgu.developers.domain.teamthread.application.command;

import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamThreadCommandService {

    private final TeamThreadRepository teamThreadRepository;

    /**
     * team_thread는 팀당 정확히 하나만 존재한다 (UNIQUE(team_id)).
     * 별도의 "스레드 생성" API를 두지 않고, 최초 접근 시 지연 생성한다.
     */
    public TeamThread getOrCreateThread(Long teamId) {
        return teamThreadRepository.findByTeamId(teamId)
            .orElseGet(() -> teamThreadRepository.save(TeamThread.create(teamId)));
    }
}
