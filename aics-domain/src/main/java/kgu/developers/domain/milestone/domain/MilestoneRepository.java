package kgu.developers.domain.milestone.domain;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository {
    Milestone save(Milestone milestone);

    List<Milestone> saveAll(List<Milestone> milestones);

    Optional<Milestone> findById(Long id);

    List<Milestone> findAllBySectionIdOrderByWeekNumber(Long sectionId);

    List<Milestone> findAllBySectionIdAndStatusOrderByWeekNumber(
            Long sectionId,
            MilestoneStatus status
    );
}
