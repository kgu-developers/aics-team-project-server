package kgu.developers.domain.submission.application.query;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    public Submission getSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
    }

    // 마일스톤 publish 시점에 미리 만들어두는(eager) 대신, 팀이 처음 조회하는 시점에 만든다.
    // B1(마일스톤) 쪽 코드를 안 건드리면서도 "GET .../my-team-submission은 절대 404가 안 난다"는
    // API 계약은 그대로 지킬 수 있다.
    public Submission getOrCreateSubmission(Long teamId, Long milestoneId) {
        return submissionRepository.findByTeamIdAndMilestoneId(teamId, milestoneId)
                .orElseGet(() -> createSubmission(teamId, milestoneId));
    }

    // 팀이 같은 마일스톤을 동시에 처음 조회하면 위 findByTeamIdAndMilestoneId가 둘 다 비어있는
    // 걸 보고 둘 다 저장을 시도할 수 있다 — DB 유니크 제약(uk_submission_team_milestone)이
    // 하나만 통과시키므로, 저장이 그 제약에 걸리면 방금 다른 요청이 만든 행을 다시 조회해서
    // 반환한다(멱등 처리, 두 번째 요청이 500 대신 정상 응답을 받게).
    //
    // save()도 재조회도 각자 별도의 REQUIRES_NEW 트랜잭션에서 실행한다 — 저장이 유니크 제약
    // 위반으로 실패하면 PostgreSQL은 그 트랜잭션이 물려있던 커넥션 자체를 abort 상태로 만든다.
    // 재조회만 새 트랜잭션으로 옮기고 save() 시도를 호출자의(이 메서드를 부른 쪽의) 트랜잭션
    // 안에 그대로 두면, 재조회가 새 트랜잭션에서 성공해도 호출자의 트랜잭션은 이미 abort된
    // 커넥션을 물고 있어 커밋 시점에 실패한다(sunzx0428 PR #87 리뷰 09-03 2차). save() 시도
    // 자체를 독립된 트랜잭션으로 분리해야 실패가 호출자의 트랜잭션을 오염시키지 않는다.
    private Submission createSubmission(Long teamId, Long milestoneId) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return requiresNew.execute(status -> submissionRepository.save(Submission.create(teamId, milestoneId)));
        } catch (DataIntegrityViolationException e) {
            return requiresNew.execute(status -> submissionRepository
                    .findByTeamIdAndMilestoneId(teamId, milestoneId)
                    .orElseThrow(() -> e));
        }
    }

    // 완료(COMPLETED) 처리된 제출은 공식 기간이 남아있어도 재제출을 막는다 — 재오픈되면
    // 상태가 REVISION_REQUESTED로 바뀌므로 이 가드에 안 걸리고 정상적으로 다시 열린다.
    // (교수 재오픈 전까지 COMPLETED가 SUBMITTED로 되돌아가며 completedAt/completedBy가
    // 모순되게 남는 문제 방지, sunzx0428 PR #87 3차 리뷰 2번 항목)
    public boolean canSubmitNow(Submission submission) {
        if (submission.isCompleted()) {
            return false;
        }
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
