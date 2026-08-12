package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaGradeRepository extends JpaRepository<GradeJpaEntity, Long> {
    Optional<GradeJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<GradeJpaEntity> findBySectionIdAndTeamIdAndUserIdAndDeletedAtIsNull(Long sectionId, Long teamId, String userId);

    List<GradeJpaEntity> findAllBySectionIdAndTeamIdAndDeletedAtIsNull(Long sectionId, Long teamId);
}
