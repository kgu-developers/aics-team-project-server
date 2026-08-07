package kgu.developers.domain.milestone.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMilestoneRepository extends JpaRepository<MilestoneJpaEntity, Long> {

    Optional<MilestoneJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<MilestoneJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(Long sectionId);

    List<MilestoneJpaEntity> findAllBySectionIdAndStatusAndDeletedAtIsNullOrderByWeekNumberAsc(
            Long sectionId,
            MilestoneStatus status
    );
}
