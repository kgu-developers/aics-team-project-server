package kgu.developers.api.editlock.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockUnsupportedTargetException;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.submission.exception.SubmissionNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class EditLockFacade {

    private final EditLockCommandService editLockCommandService;
    private final EditLockQueryService editLockQueryService;
    private final SubmissionRepository submissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MilestoneRepository milestoneRepository;
    private final EnrollmentRepository enrollmentRepository;

    // acquire()와 같은 대상 접근 검증을 거친다 — 검증 없이 조회를 허용하면 다른 분반·팀
    // 사용자도 lockedBy(학번)를 알아낼 수 있었다(sunzx0428 PR #87 리뷰 09-03).
    public EditLockStatusResponse getStatus(EditLockTargetType targetType, Long targetId, String userId) {
        validateTargetAccess(targetType, targetId, userId);
        return getStatusWithoutAccessCheck(targetType, targetId);
    }

    private EditLockStatusResponse getStatusWithoutAccessCheck(EditLockTargetType targetType, Long targetId) {
        return editLockQueryService.getActiveLock(targetType, targetId)
            .map(EditLockStatusResponse::from)
            .orElseGet(EditLockStatusResponse::unlocked);
    }

    public EditLockStatusResponse acquire(String userId, EditLockAcquireRequest request) {
        validateTargetAccess(request.targetType(), request.targetId(), userId);
        editLockCommandService.acquire(request.targetType(), request.targetId(), userId);
        return getStatusWithoutAccessCheck(request.targetType(), request.targetId());
    }

    public void release(EditLockTargetType targetType, Long targetId, String userId) {
        editLockCommandService.release(targetType, targetId, userId);
    }

    // 대상이 실제로 존재하고, 이 사용자가 그 대상을 편집할 권한이 있는지 확인한다.
    // targetType/targetId는 폴리모픽 참조(FK 없음)라 여기서 타입별로 갈라서 검증해야 한다.
    private void validateTargetAccess(EditLockTargetType targetType, Long targetId, String userId) {
        switch (targetType) {
            case PRESENTATION_CONTENT -> validatePresentationContentAccess(targetId, userId);
            // PROJECT(B2)는 아직 이 저장소에 도메인이 없어 검증 대상을 정할 수 없다 — 검증 없이
            // 통과시키면 아무 인증 사용자나 임의 대상을 잠글 수 있게 되므로, Project 도메인이
            // 들어오기 전까지는 아예 지원하지 않는 대상으로 명시적으로 거부한다.
            case PROJECT -> throw new EditLockUnsupportedTargetException();
        }
    }

    // PRESENTATION_CONTENT의 targetId는 submissionId다(발표자료는 제출 1건당 하나).
    // 팀원 행이 남아있는 것만으로는 부족하고, 지금 이 분반에 활성 학생으로 등록돼 있어야
    // 잠글 수 있다 — 탈퇴·조교 전환자가 잠금을 잡아 실제 편집자를 막는 걸 방지한다.
    private void validatePresentationContentAccess(Long submissionId, String userId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);
        if (teamMemberRepository.findByTeamIdAndUserId(submission.getTeamId(), userId).isEmpty()) {
            throw new AccessDeniedException("그 팀 소속만 발표자료를 편집할 수 있습니다.");
        }
        Milestone milestone = milestoneRepository.findById(submission.getMilestoneId())
                .orElseThrow(() -> new MilestoneNotFoundException(submission.getMilestoneId()));
        boolean activeStudent = enrollmentRepository.findBySectionIdAndUserId(milestone.getSectionId(), userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
        if (!activeStudent) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 편집할 수 있습니다.");
        }
    }
}
