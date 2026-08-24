package mock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;

public class FakeTeamMemberRepository implements TeamMemberRepository {

    private final Map<Long, TeamMember> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public TeamMember save(TeamMember teamMember) {
        Long id = teamMember.getId() != null ? teamMember.getId() : sequence.incrementAndGet();

        TeamMember saved = TeamMember.builder()
            .id(id)
            .teamId(teamMember.getTeamId())
            .userId(teamMember.getUserId())
            .isLeader(teamMember.isLeader())
            .projectRole(teamMember.getProjectRole())
            .createdAt(teamMember.getCreatedAt() != null ? teamMember.getCreatedAt() : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deletedAt(teamMember.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<TeamMember> findById(Long id) {
        return Optional.ofNullable(store.get(id))
            .filter(teamMember -> teamMember.getDeletedAt() == null);
    }

    @Override
    public List<TeamMember> findAllByTeamId(Long teamId) {
        return store.values().stream()
            .filter(teamMember -> teamMember.getDeletedAt() == null)
            .filter(teamMember -> teamMember.getTeamId().equals(teamId))
            .toList();
    }

    @Override
    public List<TeamMember> findAllByUserId(String userId) {
        return store.values().stream()
            .filter(teamMember -> teamMember.getDeletedAt() == null)
            .filter(teamMember -> teamMember.getUserId().equals(userId))
            .toList();
    }

    @Override
    public Optional<TeamMember> findByTeamIdAndUserId(Long teamId, String userId) {
        return store.values().stream()
            .filter(teamMember -> teamMember.getDeletedAt() == null)
            .filter(teamMember -> teamMember.getTeamId().equals(teamId))
            .filter(teamMember -> teamMember.getUserId().equals(userId))
            .findFirst();
    }

    @Override
    public Optional<TeamMember> findIncludingDeleted(Long teamId, String userId) {
        return store.values().stream()
            .filter(teamMember -> teamMember.getTeamId().equals(teamId))
            .filter(teamMember -> teamMember.getUserId().equals(userId))
            .findFirst();
    }

    @Override
    public Optional<TeamMember> findLeaderByTeamId(Long teamId) {
        return store.values().stream()
            .filter(teamMember -> teamMember.getDeletedAt() == null)
            .filter(teamMember -> teamMember.getTeamId().equals(teamId))
            .filter(TeamMember::isLeader)
            .findFirst();
    }

    @Override
    public boolean existsByTeamIdAndIsLeaderTrue(Long teamId) {
        return findLeaderByTeamId(teamId).isPresent();
    }

    @Override
    public Optional<TeamMember> findActiveBySectionIdAndUserId(Long sectionId, String userId) {
        return Optional.empty();
    }

    @Override
    public void deleteById(Long id) {
        Optional.ofNullable(store.get(id)).ifPresent(teamMember -> teamMember.delete());
    }

    @Override
    public void deleteAllByTeamId(Long teamId) {
        store.values().stream()
            .filter(teamMember -> teamMember.getTeamId().equals(teamId))
            .forEach(TeamMember::delete);
    }
}
