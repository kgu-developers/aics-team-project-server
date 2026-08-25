package kgu.developers.domain.project.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectJpaEntity p where p.id = :id and p.deletedAt is null")
    Optional<ProjectJpaEntity> findByIdForUpdate(@Param("id") Long id);

    List<ProjectJpaEntity> findAllByTeamIdAndDeletedAtIsNull(Long teamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectJpaEntity p where p.team.id = :teamId and p.deletedAt is null")
    List<ProjectJpaEntity> findAllByTeamIdForUpdate(@Param("teamId") Long teamId);

    List<ProjectJpaEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
