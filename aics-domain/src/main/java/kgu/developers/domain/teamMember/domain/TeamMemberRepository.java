package kgu.developers.domain.teamMember.domain;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository {
    TeamMember save(TeamMember teamMember);

    Optional<TeamMember> findById(Long id);

    List<TeamMember> findAllByTeamId(Long teamId);

    List<TeamMember> findAllByUserId(String userId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, String userId);

    Optional<TeamMember> findIncludingDeleted(Long teamId, String userId);

    Optional<TeamMember> findLeaderByTeamId(Long teamId);

    boolean existsByTeamIdAndIsLeaderTrue(Long teamId);

    void deleteById(Long id);

    void deleteAllByTeamId(Long teamId);
}
