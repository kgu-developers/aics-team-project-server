package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;

public class FakeTeamRepository implements TeamRepository {

    private final Map<Long, Team> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Team save(Team team) {
        Long id = team.getId() != null ? team.getId() : sequence.incrementAndGet();

        Team saved = Team.builder()
            .id(id)
            .sectionId(team.getSectionId())
            .name(team.getName())
            .kickoffRule(team.getKickoffRule())
            .meetingSchedule(team.getMeetingSchedule())
            .status(team.getStatus())
            .createdAt(team.getCreatedAt())
            .updatedAt(team.getUpdatedAt())
            .deletedAt(team.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Team> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Team> findAllById(List<Long> ids) {
        return store.values().stream()
            .filter(team -> ids.contains(team.getId()))
            .toList();
    }

    @Override
    public List<Team> findAllBySectionId(Long sectionId) {
        return store.values().stream()
            .filter(team -> team.getSectionId().equals(sectionId))
            .toList();
    }

    @Override
    public List<Team> findAllBySectionIdIn(List<Long> sectionIds) {
        return store.values().stream()
            .filter(team -> sectionIds.contains(team.getSectionId()))
            .filter(team -> team.getDeletedAt() == null)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
