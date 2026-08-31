package kgu.developers.domain.milestone.domain;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository {
    Milestone save(Milestone milestone);

    List<Milestone> saveAllWeekNumberChanges(Long sectionId, List<Milestone> milestones);

    Optional<Milestone> findById(Long id);

    Optional<Milestone> findByIdAndSectionId(Long id, Long sectionId);

    Optional<Milestone> findByIdAndSectionIdForUpdate(Long id, Long sectionId);

    boolean existsBySectionIdAndWeekNumber(Long sectionId, int weekNumber);

    List<Milestone> findAllBySectionIdOrderByWeekNumber(Long sectionId);

    List<Milestone> findAllBySectionIdAndStatusOrderByWeekNumber(
            Long sectionId,
            MilestoneStatus status
    );
}
