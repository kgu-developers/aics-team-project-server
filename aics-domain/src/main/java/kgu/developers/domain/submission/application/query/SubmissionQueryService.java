package kgu.developers.domain.submission.application.query;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.exception.SubmissionNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionQueryService {
    private final SubmissionRepository submissionRepository;
    private final MilestoneRepository milestoneRepository;

    public Submission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
    }

    // 마일스톤 publish 시점에 미리 만들어두는(eager) 대신, 팀이 처음 조회하는 시점에 만든다.
    // B1(마일스톤) 쪽 코드를 안 건드리면서도 "GET .../my-team-submission은 절대 404가 안 난다"는
    // API 계약은 그대로 지킬 수 있다.
    @Transactional
    public Submission getOrCreateSubmission(Long teamId, Long milestoneId) {
        return submissionRepository.findByTeamIdAndMilestoneId(teamId, milestoneId)
                .orElseGet(() -> submissionRepository.save(Submission.create(teamId, milestoneId)));
    }

    public boolean canSubmitNow(Submission submission) {
        Milestone milestone = getMilestone(submission.getMilestoneId());
        if (withinOwnWindow(milestone)) {
            return true;
        }
        if (withinReopenedWindow(submission)) {
            return true;
        }
        return nextMilestoneOpenedEarly(milestone, submission.getTeamId());
    }

    // 교수가 재오픈하면서 잡아준 재제출 기한이 아직 안 지났으면, 공식 기간이 끝났어도 허용한다.
    private boolean withinReopenedWindow(Submission submission) {
        return isBefore(LocalDateTime.now(), submission.getRevisionDueAt());
    }

    // 발표 마일스톤의 팀별 제출을 발표순서로 정렬해서 반환한다(순서 미지정 팀은 뒤로).
    public List<Submission> getSubmissionsOrderedForPresentation(Long milestoneId) {
        return submissionRepository.findAllByMilestoneId(milestoneId).stream()
                .sorted(Comparator.comparing(
                        Submission::getPresentationOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public boolean hasPendingReview(Submission submission) {
        return submission.getStatus() == SubmissionStatus.REVISION_REQUESTED;
    }

    // 공개(PUBLISHED) 상태가 아니거나, 아직 opensAt 전이면 제출 기간 자체가 시작 안 한 것이다.
    private boolean withinOwnWindow(Milestone milestone) {
        if (milestone.getStatus() != MilestoneStatus.PUBLISHED) {
            return false;
        }
        MilestoneSchedule schedule = milestone.getSchedule();
        LocalDateTime now = LocalDateTime.now();
        if (!hasOpened(now, schedule.opensAt())) {
            return false;
        }
        return isBefore(now, schedule.dueAt())
                || isBefore(now, schedule.lateSubmissionUntil())
                || isBefore(now, schedule.revisionUntil());
    }

    private boolean hasOpened(LocalDateTime now, LocalDateTime opensAt) {
        return opensAt == null || !now.isBefore(opensAt);
    }

    // 팀이 직전 마일스톤(같은 분반의 바로 앞 주차) 제출을 이미 끝냈으면, 이 마일스톤의
    // 공식 오픈일(opensAt) 전이라도 그 팀에 한해 미리 열어준다(팀별 조기활성화, 전체 공개일은 그대로).
    // opensAt이 이미 지났으면(또는 애초에 없으면) "조기"라는 개념 자체가 성립하지 않으므로
    // withinOwnWindow의 판단에 맡기고 여기서는 항상 false를 준다 — 안 그러면 모든 기한이
    // 지난 뒤에도 이 조건만으로 계속 제출 가능 상태가 유지되는 문제가 생긴다.
    private boolean nextMilestoneOpenedEarly(Milestone milestone, Long teamId) {
        if (milestone.getStatus() != MilestoneStatus.PUBLISHED) {
            return false;
        }
        LocalDateTime opensAt = milestone.getSchedule().opensAt();
        if (opensAt == null || !LocalDateTime.now().isBefore(opensAt)) {
            return false;
        }

        List<Milestone> sectionMilestones = milestoneRepository
                .findAllBySectionIdOrderByWeekNumber(milestone.getSectionId());

        Optional<Milestone> previous = sectionMilestones.stream()
                .filter(m -> m.getWeekNumber() < milestone.getWeekNumber())
                .filter(m -> m.getStatus() != MilestoneStatus.DRAFT)
                .max((a, b) -> Integer.compare(a.getWeekNumber(), b.getWeekNumber()));

        if (previous.isEmpty()) {
            return false;
        }

        return submissionRepository.findByTeamIdAndMilestoneId(teamId, previous.get().getId())
                .map(previousSubmission -> previousSubmission.getStatus() != SubmissionStatus.NOT_SUBMITTED)
                .orElse(false);
    }

    private boolean isBefore(LocalDateTime now, LocalDateTime bound) {
        return bound != null && now.isBefore(bound);
    }

    private Milestone getMilestone(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));
    }
}
