package editlock.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import kgu.developers.api.editlock.application.EditLockFacade;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.teamMember.domain.TeamMember;
import mock.repository.FakeEditLockRepository;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeTeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EditLockFacadeTest {

    private static final String MEMBER = "202412345";
    private static final String OTHER_MEMBER = "202499999";
    private static final EditLockTargetType TARGET_TYPE = EditLockTargetType.PRESENTATION_CONTENT;
    private static final Long SECTION_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long MILESTONE_ID = 100L;
    private static final Long TARGET_ID = 1L;
    private static final String SECTION_KEY = "DEFAULT";

    private EditLockFacade facade;

    @BeforeEach
    public void init() {
        FakeEditLockRepository fakeEditLockRepository = new FakeEditLockRepository();
        FakeSubmissionRepository fakeSubmissionRepository = new FakeSubmissionRepository();
        FakeTeamMemberRepository fakeTeamMemberRepository = new FakeTeamMemberRepository();
        FakeEnrollmentRepository fakeEnrollmentRepository = new FakeEnrollmentRepository();
        MilestoneRepository milestoneRepository = mock(MilestoneRepository.class);

        // TARGET_ID(=1L)와 실제로 매칭되도록, 시퀀스가 1부터 시작하는 이 Fake의 첫 저장 결과를 그대로 씀.
        Submission submission = fakeSubmissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        fakeTeamMemberRepository.save(TeamMember.create(submission.getTeamId(), MEMBER, false, "팀원"));
        fakeTeamMemberRepository.save(TeamMember.create(submission.getTeamId(), OTHER_MEMBER, false, "팀원"));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 1, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, java.time.LocalDateTime.now().plusDays(1), null, null, null, null))));
        fakeEnrollmentRepository.save(Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE));
        fakeEnrollmentRepository.save(Enrollment.create(SECTION_ID, OTHER_MEMBER, Role.STUDENT, Status.ACTIVE));

        facade = new EditLockFacade(
            new EditLockCommandService(fakeEditLockRepository),
            new EditLockQueryService(fakeEditLockRepository),
            fakeSubmissionRepository,
            fakeTeamMemberRepository,
            milestoneRepository,
            fakeEnrollmentRepository
        );
    }

    private EditLockAcquireRequest buildRequest() {
        return buildRequest(SECTION_KEY);
    }

    private EditLockAcquireRequest buildRequest(String sectionKey) {
        return EditLockAcquireRequest.builder()
            .targetType(TARGET_TYPE)
            .targetId(TARGET_ID)
            .sectionKey(sectionKey)
            .build();
    }

    @Test
    @DisplayName("getStatus는 잠금이 없으면 locked=false를 반환한다")
    public void getStatus_Unlocked() {
        // when
        EditLockStatusResponse result = facade.getStatus(TARGET_TYPE, TARGET_ID, SECTION_KEY, MEMBER);

        // then
        assertFalse(result.locked());
    }

    @Test
    @DisplayName("acquire는 잠금을 획득하고 상태를 반환한다")
    public void acquire_Success() {
        // when
        EditLockStatusResponse result = facade.acquire(MEMBER, buildRequest());

        // then
        assertTrue(result.locked());
        assertEquals(MEMBER, result.lockedBy());
    }

    @Test
    @DisplayName("acquire는 타인이 잠그고 있으면 예외를 던진다")
    public void acquire_Conflict_ThrowsException() {
        // given
        facade.acquire(MEMBER, buildRequest());

        // when & then
        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, buildRequest()))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("release 후에는 getStatus가 locked=false를 반환한다")
    public void release_ThenGetStatus_Unlocked() {
        // given
        facade.acquire(MEMBER, buildRequest());

        // when
        facade.release(TARGET_TYPE, TARGET_ID, SECTION_KEY, MEMBER);

        // then
        assertFalse(facade.getStatus(TARGET_TYPE, TARGET_ID, SECTION_KEY, MEMBER).locked());
    }

    @Test
    @DisplayName("같은 대상이어도 섹션이 다르면 서로 다른 사람이 동시에 잠글 수 있다")
    public void acquire_DifferentSection_DoesNotConflict() {
        // when
        facade.acquire(MEMBER, buildRequest("TEAM_INFO"));
        EditLockStatusResponse otherSection = facade.acquire(OTHER_MEMBER, buildRequest("TOPIC"));

        // then
        assertTrue(otherSection.locked());
        assertEquals(OTHER_MEMBER, otherSection.lockedBy());
        assertTrue(facade.getStatus(TARGET_TYPE, TARGET_ID, "TEAM_INFO", MEMBER).locked());
    }

    @Test
    @DisplayName("존재하지 않는 대상은 잠글 수 없다")
    public void acquire_RejectsMissingTarget() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(TARGET_TYPE)
            .targetId(9999L)
            .sectionKey(SECTION_KEY)
            .build();

        assertThatThrownBy(() -> facade.acquire(MEMBER, request))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getStatus는 그 팀 소속이 아니면 조회를 거부한다")
    public void getStatus_RejectsNonMember() {
        String outsider = "202400000";

        assertThatThrownBy(() -> facade.getStatus(TARGET_TYPE, TARGET_ID, SECTION_KEY, outsider))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("PROJECT 대상은 아직 지원하지 않아 거부된다")
    public void acquire_RejectsUnsupportedProjectTarget() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.PROJECT)
            .targetId(TARGET_ID)
            .sectionKey(SECTION_KEY)
            .build();

        assertThatThrownBy(() -> facade.acquire(MEMBER, request))
            .isInstanceOf(CustomException.class);
    }
}
