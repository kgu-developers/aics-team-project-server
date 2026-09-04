package kgu.developers.domain.team.infrastructure;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTeamRepository extends JpaRepository<TeamJpaEntity, Long> {
    Optional<TeamJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamJpaEntity> findAllBySectionIdAndDeletedAtIsNull(Long sectionId);

    List<TeamJpaEntity> findAllBySectionIdInAndDeletedAtIsNull(List<Long> sectionIds);

    List<TeamJpaEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    boolean existsBySectionIdAndNameAndIdNotAndDeletedAtIsNull(Long sectionId, String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TeamJpaEntity t where t.id = :id and t.deletedAt is null")
    Optional<TeamJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
