package kgu.developers.domain.team.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTeamRepository extends JpaRepository<TeamJpaEntity, Long> {
    Optional<TeamJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamJpaEntity> findAllBySectionIdAndDeletedAtIsNull(Long sectionId);

    List<TeamJpaEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
