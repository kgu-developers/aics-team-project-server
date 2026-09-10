package kgu.developers.api.editlock.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class EditLockFacade {

    private final EditLockCommandService editLockCommandService;
    private final EditLockQueryService editLockQueryService;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MeetingRecordQueryService meetingRecordQueryService;
    private final UserQueryService userQueryService;

    // acquire()와 같은 대상 접근 검증을 거친다 — 검증 없이 조회를 허용하면 다른 분반·팀
    // 사용자도 lockedBy(학번)를 알아낼 수 있었다(sunzx0428 PR #87 리뷰 09-03).
    public EditLockStatusResponse getStatus(EditLockTargetType targetType, Long targetId, String sectionKey, String userId) {
        validateTargetAccess(targetType, targetId, userId);
        return getStatusWithoutAccessCheck(targetType, targetId, sectionKey);
    }

    private EditLockStatusResponse getStatusWithoutAccessCheck(EditLockTargetType targetType, Long targetId, String sectionKey) {
        return editLockQueryService.getActiveLock(targetType, targetId, sectionKey)
            .map(lock -> {
                String userName = resolveUserName(lock.getLockedBy());
                return EditLockStatusResponse.from(lock, userName);
            })
            .orElseGet(EditLockStatusResponse::unlocked);
    }

    private String resolveUserName(String studentNumber) {
        if (studentNumber == null || studentNumber.isBlank()) {
            return null;
        }
        try {
            return userQueryService.getUserByStudentNumber(studentNumber).getName();
        } catch (UserNotFoundException e) {
            return null;
        }
    }

    public EditLockStatusResponse acquire(String userId, EditLockAcquireRequest request) {
        validateTargetAccess(request.targetType(), request.targetId(), userId);
        editLockCommandService.acquire(request.targetType(), request.targetId(), request.sectionKey(), userId);
        return getStatusWithoutAccessCheck(request.targetType(), request.targetId(), request.sectionKey());
    }

    public void release(EditLockTargetType targetType, Long targetId, String sectionKey, String userId) {
        editLockCommandService.release(targetType, targetId, sectionKey, userId);
    }

    // 대상이 실제로 존재하고, 이 사용자가 그 대상을 편집할 권한이 있는지 확인한다.
    // targetType/targetId는 폴리모픽 참조(FK 없음)라 여기서 타입별로 갈라서 검증해야 한다.
    private void validateTargetAccess(EditLockTargetType targetType, Long targetId, String userId) {
        switch (targetType) {
            case PROJECT -> validateProjectAccess(targetId, userId);
            case MEETING_RECORD -> validateMeetingRecordAccess(targetId, userId);
        }
    }

    // PROJECT의 targetId는 projectId다.
    // 팀원 행이 남아있는 것만으로는 부족하고, 지금 이 분반에 활성 학생으로 등록돼 있어야
    // 잠글 수 있다 — 탈퇴·조교 전환자가 잠금을 잡아 실제 편집자를 막는 걸 방지한다.
    private void validateProjectAccess(Long projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        Team team = teamRepository.findById(project.getTeamId())
                .orElseThrow(TeamNotFoundException::new);
        if (teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId).isEmpty()) {
            throw new AccessDeniedException("그 팀 소속만 프로젝트를 편집할 수 있습니다.");
        }
        boolean activeStudent = enrollmentRepository.findBySectionIdAndUserId(team.getSectionId(), userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
        if (!activeStudent) {
            throw new AccessDeniedException("그 분반에 활성 학생으로 등록된 사용자만 편집할 수 있습니다.");
        }
    }

    // MEETING_RECORD의 targetId는 meetingRecordId다.
    // 해당 팀 소속이어야 하고, 그 분반에 활성 학생으로 등록돼 있어야 잠글 수 있다.
    private void validateMeetingRecordAccess(Long meetingRecordId, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(meetingRecordId);
        if (teamMemberRepository.findByTeamIdAndUserId(meetingRecord.getTeamId(), userId).isEmpty()) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 회의록을 편집할 수 있습니다.");
        }
        Team team = teamRepository.findById(meetingRecord.getTeamId())
                .orElseThrow(TeamNotFoundException::new);
        boolean activeStudent = enrollmentRepository.findBySectionIdAndUserId(team.getSectionId(), userId)
                .map(Enrollment::isActiveStudent)
                .orElse(false);
        if (!activeStudent) {
            throw new AccessDeniedException("해당 분반의 활성 학생만 회의록을 편집할 수 있습니다.");
        }
    }
}
