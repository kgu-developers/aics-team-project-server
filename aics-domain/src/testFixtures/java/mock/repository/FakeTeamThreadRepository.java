package mock.repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;

public class FakeTeamThreadRepository implements TeamThreadRepository {

    private final Map<Long, TeamThread> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public TeamThread save(TeamThread teamThread) {
        Long id = teamThread.getId() != null ? teamThread.getId() : sequence.incrementAndGet();
        TeamThread saved = TeamThread.builder()
            .id(id)
            .teamId(teamThread.getTeamId())
            .createdAt(teamThread.getCreatedAt() != null ? teamThread.getCreatedAt() : LocalDateTime.now())
            .build();
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<TeamThread> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<TeamThread> findByTeamId(Long teamId) {
        return store.values().stream()
            .filter(thread -> thread.getTeamId().equals(teamId))
            .findFirst();
    }
}
