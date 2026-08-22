package kgu.developers.domain.milestone.domain;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository {
    Milestone save(Milestone milestone);

    /** 주차 일괄 변경 시 해당 분반의 활성 마일스톤 전체를 전달해야 한다. */
    List<Milestone> saveAll(List<Milestone> milestones);

    Optional<Milestone> findById(Long id);

    Optional<Milestone> findByIdForUpdate(Long id);

    List<Milestone> findAllBySectionIdOrderByWeekNumber(Long sectionId);

    List<Milestone> findAllBySectionIdForUpdateOrderByWeekNumber(Long sectionId);

    List<Milestone> findAllBySectionIdAndStatusOrderByWeekNumber(
            Long sectionId,
            MilestoneStatus status
    );
}
