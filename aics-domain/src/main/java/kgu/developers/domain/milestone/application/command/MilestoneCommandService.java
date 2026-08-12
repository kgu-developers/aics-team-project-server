package kgu.developers.domain.milestone.application.command;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.exception.MilestoneSectionMismatchException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MilestoneCommandService {
    private final MilestoneRepository milestoneRepository;

    public Long createMilestone(
            Long sectionId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule
    ) {
        Milestone milestone = Milestone.create(sectionId, title, description, weekNumber, schedule);
        Milestone savedMilestone = milestoneRepository.save(milestone);

        if (savedMilestone.getId() == null) {
            throw new IllegalStateException("저장된 마일스톤 식별자가 없습니다.");
        }
        return savedMilestone.getId();
    }

    public void updateMilestone(
            Long sectionId,
            Long milestoneId,
            String title,
            String description,
            MilestoneSchedule schedule
    ) {
        validateSchedule(schedule);
        Milestone milestone = getRequiredMilestone(sectionId, milestoneId);
        milestone.updateDetails(title, description);
        milestone.updateSchedule(schedule);
        milestoneRepository.save(milestone);
    }

    public void changeStatus(Long sectionId, Long milestoneId, MilestoneStatus status) {
        validateStatus(status);
        Milestone milestone = getRequiredMilestone(sectionId, milestoneId);
        milestone.changeStatus(status);
        milestoneRepository.save(milestone);
    }

    public void updateEvaluationWindow(
            Long sectionId,
            Long milestoneId,
            LocalDateTime evaluationOpensAt,
            LocalDateTime evaluationClosesAt
    ) {
        Milestone milestone = getRequiredMilestone(sectionId, milestoneId);
        milestone.updateEvaluationWindow(evaluationOpensAt, evaluationClosesAt);
        milestoneRepository.save(milestone);
    }

    public void updateWeekNumbers(Long sectionId, List<MilestoneWeekNumberChange> changes) {
        validateSectionId(sectionId);
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("변경할 마일스톤 주차가 필요합니다.");
        }

        List<Milestone> sectionMilestones = milestoneRepository
                .findAllBySectionIdForUpdateOrderByWeekNumber(sectionId);
        Map<Long, Milestone> sectionMilestonesById = new HashMap<>();
        Map<Long, Integer> finalWeekNumbersById = new HashMap<>();
        for (Milestone milestone : sectionMilestones) {
            sectionMilestonesById.put(milestone.getId(), milestone);
            finalWeekNumbersById.put(milestone.getId(), milestone.getWeekNumber());
        }

        Set<Long> requestedIds = new HashSet<>();
        List<Milestone> milestones = new ArrayList<>(changes.size());
        for (MilestoneWeekNumberChange change : changes) {
            if (change == null) {
                throw new IllegalArgumentException("주차 변경 항목은 null일 수 없습니다.");
            }
            if (!requestedIds.add(change.milestoneId())) {
                throw new IllegalArgumentException("같은 마일스톤의 주차를 두 번 변경할 수 없습니다.");
            }

            Milestone milestone = getRequiredSectionMilestone(
                    sectionId,
                    change.milestoneId(),
                    sectionMilestonesById
            );
            milestones.add(milestone);
            finalWeekNumbersById.put(change.milestoneId(), change.weekNumber());
        }

        validateUniqueWeekNumbers(finalWeekNumbersById.values());
        for (int index = 0; index < milestones.size(); index++) {
            milestones.get(index).changeWeekNumber(changes.get(index).weekNumber());
        }
        milestoneRepository.saveAll(milestones);
    }

    private Milestone getRequiredSectionMilestone(
            Long sectionId,
            Long milestoneId,
            Map<Long, Milestone> sectionMilestonesById
    ) {
        Milestone milestone = sectionMilestonesById.get(milestoneId);
        if (milestone != null) {
            return milestone;
        }

        milestone = getRequiredMilestone(milestoneId);
        if (!milestone.belongsToSection(sectionId)) {
            throw new MilestoneSectionMismatchException(milestoneId, sectionId);
        }
        sectionMilestonesById.put(milestoneId, milestone);
        return milestone;
    }

    private void validateUniqueWeekNumbers(Iterable<Integer> weekNumbers) {
        Set<Integer> uniqueWeekNumbers = new HashSet<>();
        for (Integer weekNumber : weekNumbers) {
            if (!uniqueWeekNumbers.add(weekNumber)) {
                throw new IllegalArgumentException("같은 분반에서 주차를 중복할 수 없습니다.");
            }
        }
    }

    private Milestone getRequiredMilestone(Long milestoneId) {
        validateMilestoneId(milestoneId);
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
    }

    private Milestone getRequiredMilestone(Long sectionId, Long milestoneId) {
        validateSectionId(sectionId);
        Milestone milestone = getRequiredMilestone(milestoneId);
        if (!milestone.belongsToSection(sectionId)) {
            throw new MilestoneSectionMismatchException(milestoneId, sectionId);
        }
        return milestone;
    }

    private void validateMilestoneId(Long milestoneId) {
        if (milestoneId == null || milestoneId <= 0) {
            throw new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다.");
        }
    }

    private void validateStatus(MilestoneStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("공개 상태는 필수입니다.");
        }
    }

    private void validateSchedule(MilestoneSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("마일스톤 일정은 필수입니다.");
        }
    }

    private void validateSectionId(Long sectionId) {
        if (sectionId == null || sectionId <= 0) {
            throw new IllegalArgumentException("분반 식별자는 양수여야 합니다.");
        }
    }
}
