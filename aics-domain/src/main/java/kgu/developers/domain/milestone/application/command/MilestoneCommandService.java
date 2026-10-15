package kgu.developers.domain.milestone.application.command;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationRepository;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.DuplicateMilestoneWeekException;
import kgu.developers.domain.milestone.exception.MilestoneHasOperationalDataException;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.exception.MilestoneEvaluationWindowConflictException;
import kgu.developers.domain.milestone.exception.MilestoneSectionAccessDeniedException;
import kgu.developers.domain.milestone.exception.MilestoneStatusNotDraftException;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MilestoneCommandService {
    private final MilestoneRepository milestoneRepository;
    private final SectionRepository sectionRepository;
    private final RequiredArtifactRepository requiredArtifactRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamEvaluationRepository teamEvaluationRepository;
    private final ProfessorPresentationEvaluationRepository professorPresentationEvaluationRepository;
    private final PeerEvaluationFormRepository peerEvaluationFormRepository;
    private final PeerEvaluationSubmissionRepository peerEvaluationSubmissionRepository;
    private final MidReportRepository midReportRepository;
    private final Clock serviceClock;

    public Long createMilestone(
            Long sectionId,
            String professorId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule
    ) {
        return createMilestone(
                sectionId, professorId, title, description, weekNumber, schedule, MilestoneType.GENERAL, false);
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
        return createMilestone(sectionId, professorId, title, description, weekNumber, schedule, type, false);
    }

    public Long createMilestone(
            Long sectionId,
            String professorId,
            String title,
            String description,
            int weekNumber,
            MilestoneSchedule schedule,
            MilestoneType type,
            boolean allowResubmissionBeforeDueAt
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = Milestone.create(
                sectionId,
                title,
                description,
                weekNumber,
                schedule,
                type != null ? type : MilestoneType.GENERAL,
                allowResubmissionBeforeDueAt
        );
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
        updateMilestone(sectionId, professorId, milestoneId, title, description, schedule, null, null);
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
        updateMilestone(sectionId, professorId, milestoneId, title, description, schedule, type, null);
    }

    public void updateMilestone(
            Long sectionId,
            String professorId,
            Long milestoneId,
            String title,
            String description,
            MilestoneSchedule schedule,
            MilestoneType type,
            Boolean allowResubmissionBeforeDueAt
    ) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        if (type != null && type != milestone.getType() && schedule == null) {
            throw new IllegalArgumentException("마일스톤 유형을 변경하려면 일정이 필요합니다.");
        }
        if (evaluationHasStarted(milestone)) {
            if (type != null && type != MilestoneType.PRESENTATION) {
                throw new MilestoneEvaluationWindowConflictException();
            }
            if (schedule != null && (!milestone.getSchedule().evaluationOpensAt().equals(
                    schedule.evaluationOpensAt()) || schedule.evaluationClosesAt() == null)) {
                throw new MilestoneEvaluationWindowConflictException();
            }
        }
        milestone.updateDetails(title, description);
        if (schedule != null || type != null) {
            MilestoneSchedule nextSchedule = schedule != null ? schedule : milestone.getSchedule();
            milestone.updateSchedule(nextSchedule, type != null ? type : milestone.getType());
        }
        if (allowResubmissionBeforeDueAt != null) {
            milestone.changeAllowResubmissionBeforeDueAt(allowResubmissionBeforeDueAt);
        }
        milestoneRepository.save(milestone);
    }

    public Milestone getMilestoneForUpdate(Long sectionId, String professorId, Long milestoneId) {
        lockOwnedSection(sectionId, professorId);
        return getRequiredMilestoneForUpdate(sectionId, milestoneId);
    }

    public void deleteMilestone(Long sectionId, String professorId, Long milestoneId) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        if (milestone.getStatus() != MilestoneStatus.DRAFT) {
            throw new MilestoneStatusNotDraftException();
        }
        if (hasOperationalData(milestoneId)) {
            throw new MilestoneHasOperationalDataException();
        }
        cascadeDeleteDraftConfig(milestoneId);
        milestone.delete();
        milestoneRepository.save(milestone);
    }

    private boolean hasOperationalData(Long milestoneId) {
        return hasBlockingSubmission(milestoneId)
                || !teamEvaluationRepository.findAllByMilestoneId(milestoneId).isEmpty()
                || !professorPresentationEvaluationRepository.findAllByMilestoneId(milestoneId).isEmpty()
                || hasSubmittedPeerEvaluation(milestoneId)
                || hasBlockingMidReport(milestoneId);
    }

    // 제출 현황 조회 시 자동 생성되는 NOT_SUBMITTED placeholder row는 삭제를 막지 않는다.
    private boolean hasBlockingSubmission(Long milestoneId) {
        return submissionRepository.findAllByMilestoneId(milestoneId).stream()
                .anyMatch(submission -> submission.getStatus() != SubmissionStatus.NOT_SUBMITTED
                        || submission.getPresentationOrder() != null);
    }

    // 상호평가 실제 제출은 PeerEvaluationSubmission(formId 기준)에 저장된다. 아직 제출 전인 DRAFT는 막지 않는다.
    private boolean hasSubmittedPeerEvaluation(Long milestoneId) {
        return peerEvaluationFormRepository.findByMilestoneId(milestoneId)
                .map(form -> peerEvaluationSubmissionRepository.findAllByFormId(form.getId()).stream()
                        .anyMatch(submission -> submission.getStatus() == PeerEvaluationSubmissionStatus.SUBMITTED))
                .orElse(false);
    }

    // 중간보고서는 Submission과 별개 애그리거트(teamId+milestoneId 직접 식별)라 따로 조회해야 한다.
    // 아직 제출 전인 DRAFT는 막지 않는다.
    private boolean hasBlockingMidReport(Long milestoneId) {
        return midReportRepository.findAllByMilestoneId(milestoneId).stream()
                .anyMatch(report -> report.getStatus() == MidReportStatus.SUBMITTED
                        || report.getStatus() == MidReportStatus.REVISION_REQUESTED);
    }

    private void cascadeDeleteDraftConfig(Long milestoneId) {
        for (RequiredArtifact artifact : requiredArtifactRepository.findAllByMilestoneId(milestoneId)) {
            artifact.delete();
            requiredArtifactRepository.save(artifact);
        }
        peerEvaluationFormRepository.findByMilestoneId(milestoneId).ifPresent(form -> {
            form.delete();
            peerEvaluationFormRepository.save(form);
        });
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
        if (evaluationHasStarted(milestone)
                && (evaluationOpensAt == null || !evaluationOpensAt.equals(
                        milestone.getSchedule().evaluationOpensAt()))) {
            throw new MilestoneEvaluationWindowConflictException();
        }
        milestone.updateEvaluationWindow(evaluationOpensAt, evaluationClosesAt);
        milestoneRepository.save(milestone);
    }

    public void closePresentationEvaluation(Long sectionId, String professorId, Long milestoneId) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        LocalDateTime now = LocalDateTime.now(serviceClock);
        LocalDateTime opensAt = milestone.getSchedule().evaluationOpensAt();
        LocalDateTime closesAt = milestone.getSchedule().evaluationClosesAt();
        if (milestone.getType() != MilestoneType.PRESENTATION || opensAt == null
                || !now.isAfter(opensAt) || closesAt == null || !now.isBefore(closesAt)) {
            throw new MilestoneEvaluationWindowConflictException();
        }
        milestone.updateEvaluationWindow(opensAt, now);
        milestoneRepository.save(milestone);
    }

    public void reopenPresentationEvaluation(
            Long sectionId, String professorId, Long milestoneId, LocalDateTime closesAt) {
        lockOwnedSection(sectionId, professorId);
        Milestone milestone = getRequiredMilestoneForUpdate(sectionId, milestoneId);
        LocalDateTime now = LocalDateTime.now(serviceClock);
        LocalDateTime opensAt = milestone.getSchedule().evaluationOpensAt();
        LocalDateTime previousClosesAt = milestone.getSchedule().evaluationClosesAt();
        if (milestone.getType() != MilestoneType.PRESENTATION || opensAt == null
                || now.isBefore(opensAt) || previousClosesAt == null || now.isBefore(previousClosesAt)
                || closesAt == null || !closesAt.isAfter(now)) {
            throw new MilestoneEvaluationWindowConflictException();
        }
        milestone.updateEvaluationWindow(opensAt, closesAt);
        milestoneRepository.save(milestone);
    }

    private boolean evaluationHasStarted(Milestone milestone) {
        LocalDateTime opensAt = milestone.getSchedule().evaluationOpensAt();
        return milestone.getType() == MilestoneType.PRESENTATION
                && opensAt != null && !opensAt.isAfter(LocalDateTime.now(serviceClock));
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
