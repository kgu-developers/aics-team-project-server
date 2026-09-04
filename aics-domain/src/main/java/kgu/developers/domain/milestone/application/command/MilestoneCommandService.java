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
import kgu.developers.domain.milestone.domain.MilestoneType;
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
        return createMilestone(sectionId, professorId, title, description, weekNumber, schedule, MilestoneType.GENERAL);
    }

    // B3(제출·이력·발표)가 마일스톤 유형(최종보고서/발표 등)을 실제로 지정할 수 있도록 추가한 오버로드.
    // 기존 호출부를 안 건드리려고 타입 없는 버전은 GENERAL로 기본 위임한다.
    public Long createMilestone(
            Long sectionId,
            String professorId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule,
            MilestoneType type
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = Milestone.create(
                sectionId, title, description, weekNumber, schedule, type != null ? type : MilestoneType.GENERAL);
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
        updateMilestone(sectionId, professorId, milestoneId, title, description, schedule, null);
    }

    // type이 null이면 기존 값을 유지한다(부분 수정) — null이 아니면 그 값으로 바꾼다.
    public void updateMilestone(
            Long sectionId,
            String professorId,
            Long milestoneId,
            String title,
            String description,
            MilestoneSchedule schedule,
            MilestoneType type
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        validateSchedule(schedule);
        milestone.updateDetails(title, description);
        milestone.updateSchedule(schedule);
        if (type != null) {
            milestone.changeType(type);
        }
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
        for (Milestone milestone : sectionMilestones) {
            sectionMilestonesById.put(milestone.getId(), milestone);
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
        }

        validateUniqueWeekNumbers(sectionMilestones, changes);
        List<Milestone> changedMilestones = new ArrayList<>();
        for (MilestoneWeekNumberChange change : changes) {
            Milestone milestone = sectionMilestonesById.get(change.milestoneId());
            if (milestone.getWeekNumber() == change.weekNumber()) {
                continue;
            }
            milestone.changeWeekNumber(change.weekNumber());
            changedMilestones.add(milestone);
        }
        if (!changedMilestones.isEmpty()) {
            milestoneRepository.saveAllWeekNumberChanges(sectionId, changedMilestones);
        }
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

    private void validateUniqueWeekNumbers(
            List<Milestone> sectionMilestones,
            List<MilestoneWeekNumberChange> changes
    ) {
        Set<Integer> uniqueWeekNumbers = new HashSet<>();
        for (Milestone milestone : sectionMilestones) {
            int weekNumber = finalWeekNumber(milestone, changes);
            if (!uniqueWeekNumbers.add(weekNumber)) {
                throw new DuplicateMilestoneWeekException();
            }
        }
    }

    private int finalWeekNumber(
            Milestone milestone,
            List<MilestoneWeekNumberChange> changes
    ) {
        return changes.stream()
                .filter(change -> milestone.getId().equals(change.milestoneId()))
                .map(MilestoneWeekNumberChange::weekNumber)
                .findFirst()
                .orElse(milestone.getWeekNumber());
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
