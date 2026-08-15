package kgu.developers.domain.teamthread.application.command;

import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamThreadCommandService {

    private final TeamThreadRepository teamThreadRepository;

    public TeamThread getOrCreateThread(Long teamId) {
        return teamThreadRepository.findByTeamId(teamId)
            .orElseGet(() -> createThread(teamId));
    }

    private TeamThread createThread(Long teamId) {
        try {
            return teamThreadRepository.save(TeamThread.create(teamId));
        } catch (DataIntegrityViolationException e) {
            // team_id 유니크 제약 위반 = 동시요청으로 다른 트랜잭션이 방금 먼저 만듦. 그걸 다시 조회해서 반환한다.
            return teamThreadRepository.findByTeamId(teamId)
                .orElseThrow(TeamThreadNotFoundException::new);
        }
    }
}
