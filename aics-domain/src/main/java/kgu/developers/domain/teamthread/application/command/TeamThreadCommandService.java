package kgu.developers.domain.teamthread.application.command;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TeamThreadCommandService {

    private final TeamThreadRepository teamThreadRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public TeamThreadCommandService(TeamThreadRepository teamThreadRepository, PlatformTransactionManager transactionManager) {
        this.teamThreadRepository = teamThreadRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    public TeamThread getOrCreateThread(Long teamId) {
        return teamThreadRepository.findByTeamId(teamId)
            .orElseGet(() -> createThread(teamId));
    }

    private TeamThread createThread(Long teamId) {
        try {
            // 별도 트랜잭션(REQUIRES_NEW)으로 분리한다. 같은 트랜잭션에서 insert가 실패하면
            // PostgreSQL이 그 트랜잭션 전체를 abort 상태로 만들어 버려서, 바로 아래 재조회
            // (findByTeamId)조차 거부당하고 Hibernate가 AssertionFailure로 둔갑시켜 유저에게
            // 500이 나간다(2026-10-26 실배포 중 실제 발생). REQUIRES_NEW로 분리하면 insert 실패 시
            // 이 서브 트랜잭션만 롤백되고 바깥 트랜잭션은 살아있어서 재조회가 정상 동작한다.
            return requiresNewTransactionTemplate.execute(status ->
                teamThreadRepository.save(TeamThread.create(teamId)));
        } catch (DataIntegrityViolationException e) {
            // team_id 유니크 제약 위반 = 동시요청으로 다른 트랜잭션이 방금 먼저 만듦. 그걸 다시 조회해서 반환한다.
            return teamThreadRepository.findByTeamId(teamId)
                .orElseThrow(TeamThreadNotFoundException::new);
        }
    }
}
