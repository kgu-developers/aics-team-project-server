package kgu.developers.domain.project.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<ProjectJpaEntity> findAllByTeamIdAndDeletedAtIsNull(Long teamId);

    List<ProjectJpaEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}