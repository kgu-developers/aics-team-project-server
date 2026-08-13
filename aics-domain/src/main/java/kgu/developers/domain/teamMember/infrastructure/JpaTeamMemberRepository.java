package kgu.developers.domain.teamMember.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTeamMemberRepository extends JpaRepository<TeamMemberJpaEntity, Long> {
    Optional<TeamMemberJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamMemberJpaEntity> findAllByTeamIdAndDeletedAtIsNull(Long teamId);

    List<TeamMemberJpaEntity> findAllByUserStudentNumberAndDeletedAtIsNull(String userId);

    Optional<TeamMemberJpaEntity> findByTeamIdAndUserStudentNumberAndDeletedAtIsNull(Long teamId, String userId);

    Optional<TeamMemberJpaEntity> findByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(Long teamId);

    boolean existsByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(Long teamId);
}
