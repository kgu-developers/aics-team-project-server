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
import kgu.developers.domain.milestone.exception.DuplicateMilestoneWeekException;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.exception.MilestoneSectionAccessDeniedException;
import kgu.developers.domain.section.domain.SectionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MilestoneCommandService {
    private final MilestoneRepository milestoneRepository;
    private final SectionRepository sectionRepository;

    public Long createMilestone(
            Long sectionId,
            String professorId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = Milestone.create(sectionId, title, description, weekNumber, schedule);
        if (milestoneRepository.existsBySectionIdAndWeekNumber(sectionId, weekNumber)) {
            throw new DuplicateMilestoneWeekException();
        }

        Milestone savedMilestone = milestoneRepository.save(milestone);

        if (savedMilestone.getId() == null) {
            throw new IllegalStateException("저장된 마일스톤 식별자가 없습니다.");
        }
        return savedMilestone.getId();
    }

    public void updateMilestone(
            Long sectionId,
            String professorId,
            Long milestoneId,
            String title,
            String description,
            MilestoneSchedule schedule
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        validateSchedule(schedule);
        milestone.updateDetails(title, description);
        milestone.updateSchedule(schedule);
        milestoneRepository.save(milestone);
    }

    public void changeStatus(
            Long sectionId,
            String professorId,
            Long milestoneId,
            MilestoneStatus status
    ) {
        lockOwnedSection(sectionId, professorId);
        validateStatus(status);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        milestone.changeStatus(status);
        milestoneRepository.save(milestone);
    }

    public void updateEvaluationWindow(
            Long sectionId,
            String professorId,
            Long milestoneId,
            LocalDateTime evaluationOpensAt,
            LocalDateTime evaluationClosesAt
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        milestone.updateEvaluationWindow(evaluationOpensAt, evaluationClosesAt);
        milestoneRepository.save(milestone);
    }

    public void updateWeekNumbers(
            Long sectionId,
            String professorId,
            List<MilestoneWeekNumberChange> changes
    ) {
        lockOwnedSection(sectionId, professorId);
        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("변경할 마일스톤 주차가 필요합니다.");
        }

        List<Milestone> sectionMilestones = new ArrayList<>(milestoneRepository
                .findAllBySectionIdOrderByWeekNumber(sectionId));
        Map<Long, Milestone> sectionMilestonesById = new HashMap<>();
        Map<Long, Integer> finalWeekNumbersById = new HashMap<>();
        for (Milestone milestone : sectionMilestones) {
            sectionMilestonesById.put(milestone.getId(), milestone);
            finalWeekNumbersById.put(milestone.getId(), milestone.getWeekNumber());
        }

        Set<Long> requestedIds = new HashSet<>();
        for (MilestoneWeekNumberChange change : changes) {
            if (change == null) {
                throw new IllegalArgumentException("주차 변경 항목은 null일 수 없습니다.");
            }
            if (!requestedIds.add(change.milestoneId())) {
                throw new IllegalArgumentException("같은 마일스톤의 주차를 두 번 변경할 수 없습니다.");
            }

            validateMilestoneId(change.milestoneId());
            getRequiredSectionMilestone(change.milestoneId(), sectionMilestonesById);
            finalWeekNumbersById.put(change.milestoneId(), change.weekNumber());
        }

        validateUniqueWeekNumbers(finalWeekNumbersById.values());
        for (MilestoneWeekNumberChange change : changes) {
            sectionMilestonesById.get(change.milestoneId()).changeWeekNumber(change.weekNumber());
        }
        milestoneRepository.saveAllWeekNumberChanges(sectionId, sectionMilestones);
    }

    private Milestone getRequiredSectionMilestone(
            Long milestoneId,
            Map<Long, Milestone> sectionMilestonesById
    ) {
        Milestone milestone = sectionMilestonesById.get(milestoneId);
        if (milestone == null) {
            throw new MilestoneNotFoundException(milestoneId);
        }
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

    private Milestone getRequiredMilestoneForUpdate(Long sectionId, Long milestoneId) {
        validateSectionId(sectionId);
        validateMilestoneId(milestoneId);
        return milestoneRepository
                .findByIdAndSectionIdForUpdate(milestoneId, sectionId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
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

    private void lockOwnedSection(Long sectionId, String professorId) {
        validateSectionId(sectionId);
        if (professorId == null || professorId.isBlank()) {
            throw new IllegalArgumentException("교수 식별자는 필수입니다.");
        }
        if (!sectionRepository.lockActiveByIdAndProfessorId(sectionId, professorId)) {
            throw new MilestoneSectionAccessDeniedException();
        }
    }

}
