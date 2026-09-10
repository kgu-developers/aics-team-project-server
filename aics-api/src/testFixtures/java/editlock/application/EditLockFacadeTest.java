package editlock.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.editlock.application.EditLockFacade;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.midreport.application.query.MidReportQueryService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import mock.repository.FakeEditLockRepository;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeProjectRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;

public class EditLockFacadeTest {

    private static final String MEMBER = "202412345";
    private static final String MEMBER_NAME = "홍길동";
    private static final String OTHER_MEMBER = "202499999";
    private static final String OTHER_MEMBER_NAME = "이순신";
    private static final Long SECTION_ID = 10L;
    private static final Long TEAM_ID = 100L;
    private static final Long PROJECT_ID = 1L;
    private static final Long MEETING_RECORD_ID = 200L;
    private static final Long MID_REPORT_ID = 300L;
    private static final String SECTION_KEY = "DEFAULT";

    private EditLockFacade facade;
    private FakeProjectRepository projectRepository;
    private FakeTeamRepository teamRepository;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeEnrollmentRepository enrollmentRepository;
    private MeetingRecordQueryService meetingRecordQueryService;
    private MidReportQueryService midReportQueryService;
    private UserQueryService userQueryService;

    @BeforeEach
    public void init() {
        FakeEditLockRepository fakeEditLockRepository = new FakeEditLockRepository();
        projectRepository = new FakeProjectRepository();
        teamRepository = new FakeTeamRepository();
        teamMemberRepository = new FakeTeamMemberRepository();
        enrollmentRepository = new FakeEnrollmentRepository();
        meetingRecordQueryService = mock(MeetingRecordQueryService.class);
        midReportQueryService = mock(MidReportQueryService.class);
        userQueryService = mock(UserQueryService.class);

        MeetingRecord meetingRecord = MeetingRecord.builder()
            .id(MEETING_RECORD_ID)
            .teamId(TEAM_ID)
            .title("1차 회의록")
            .authorId(MEMBER)
            .build();
        given(meetingRecordQueryService.getMeetingRecord(MEETING_RECORD_ID)).willReturn(meetingRecord);

        MidReport midReport = MidReport.builder()
            .id(MID_REPORT_ID)
            .teamId(TEAM_ID)
            .milestoneId(1L)
            .title("중간보고서")
            .dueDate(java.time.LocalDateTime.now().plusDays(7))
            .status(MidReportStatus.DRAFT)
            .version(1L)
            .blocks(java.util.List.of())
            .build();
        given(midReportQueryService.getById(MID_REPORT_ID)).willReturn(midReport);
        given(midReportQueryService.getById(999999L)).willThrow(new MidReportNotFoundException());

        given(userQueryService.getUserByStudentNumber(MEMBER))
            .willReturn(User.builder().studentNumber(MEMBER).name(MEMBER_NAME).build());
        given(userQueryService.getUserByStudentNumber(OTHER_MEMBER))
            .willReturn(User.builder().studentNumber(OTHER_MEMBER).name(OTHER_MEMBER_NAME).build());

        facade = new EditLockFacade(
            new EditLockCommandService(fakeEditLockRepository),
            new EditLockQueryService(fakeEditLockRepository),
            projectRepository,
            teamRepository,
            teamMemberRepository,
            enrollmentRepository,
            meetingRecordQueryService,
            midReportQueryService,
            userQueryService
        );

        teamRepository.save(Team.builder()
            .id(TEAM_ID)
            .sectionId(SECTION_ID)
            .name("A팀")
            .build());

        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(MEMBER)
            .build());

        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        projectRepository.save(Project.builder()
            .id(PROJECT_ID)
            .teamId(TEAM_ID)
            .title("테스트 프로젝트")
            .description("설명")
            .goal("목표")
            .approvalStatus(ApprovalStatus.PENDING)
            .build());
    }

    private EditLockAcquireRequest buildRequest() {
        return buildRequest(SECTION_KEY);
    }

    private EditLockAcquireRequest buildRequest(String sectionKey) {
        return EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.PROJECT)
            .targetId(PROJECT_ID)
            .sectionKey(sectionKey)
            .build();
    }

    @Test
    @DisplayName("getStatus는 잠금이 없으면 locked=false를 반환한다")
    public void getStatus_Unlocked() {
        EditLockStatusResponse result = facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        assertFalse(result.locked());
    }

    @Test
    @DisplayName("acquire는 타인이 잠그고 있으면 예외를 던진다")
    public void acquire_Conflict_ThrowsException() {
        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(OTHER_MEMBER)
            .build());
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        facade.acquire(MEMBER, buildRequest());

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, buildRequest()))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("같은 대상이어도 섹션이 다르면 서로 다른 사람이 동시에 잠글 수 있다")
    public void acquire_DifferentSection_DoesNotConflict() {
        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(OTHER_MEMBER)
            .build());
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        facade.acquire(MEMBER, buildRequest("TEAM_INFO"));
        EditLockStatusResponse otherSection = facade.acquire(OTHER_MEMBER, buildRequest("TOPIC"));

        assertTrue(otherSection.locked());
        assertEquals(OTHER_MEMBER, otherSection.lockedBy());
        assertTrue(facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, "TEAM_INFO", MEMBER).locked());
    }

    @Test
    @DisplayName("해당 팀의 활성 학생은 프로젝트 편집 잠금을 획득할 수 있다")
    public void acquire_Success_WhenActiveTeamMember() {
        EditLockStatusResponse response = facade.acquire(MEMBER, buildRequest());

        assertThat(response.locked()).isTrue();
        assertThat(response.lockedBy()).isEqualTo(MEMBER);
    }

    @Test
    @DisplayName("잠금 상태를 조회하면 현재 편집자 정보를 반환한다")
    public void getStatus_Success_WhenLocked() {
        facade.acquire(MEMBER, buildRequest());

        EditLockStatusResponse status = facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        assertThat(status.locked()).isTrue();
        assertThat(status.lockedBy()).isEqualTo(MEMBER);
    }

    @Test
    @DisplayName("잠금을 해제하면 미잠금 상태가 된다")
    public void release_Success() {
        facade.acquire(MEMBER, buildRequest());
        facade.release(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        EditLockStatusResponse status = facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        assertThat(status.locked()).isFalse();
    }

    @Test
    @DisplayName("팀 소속이 아닌 사용자는 프로젝트 잠금을 획득할 수 없다")
    public void acquire_RejectsNonMember() {
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, buildRequest()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("그 팀 소속만 프로젝트를 편집할 수 있습니다.");
    }

    @Test
    @DisplayName("팀원 행이 있어도 활성 학생이 아니면 프로젝트 잠금을 획득할 수 없다")
    public void acquire_RejectsInactiveStudent() {
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.WITHDRAWN)
            .build());
        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(OTHER_MEMBER)
            .build());

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, buildRequest()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("그 분반에 활성 학생으로 등록된 사용자만 편집할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 ID로 잠금을 획득하려 하면 ProjectNotFoundException이 발생한다")
    public void acquire_RejectsNonExistentProject() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.PROJECT)
            .targetId(999999L)
            .sectionKey(SECTION_KEY)
            .build();

        assertThatThrownBy(() -> facade.acquire(MEMBER, request))
            .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("팀 소속이 아닌 사용자는 잠금 상태 조회도 거부된다")
    public void getStatus_RejectsNonMember() {
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        assertThatThrownBy(() -> facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, OTHER_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("회의록에 대해 acquire는 잠금을 획득하고 lockedByName을 포함해 반환한다")
    public void acquire_MeetingRecord_Success() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MEETING_RECORD)
            .targetId(MEETING_RECORD_ID)
            .sectionKey(SECTION_KEY)
            .build();

        EditLockStatusResponse result = facade.acquire(MEMBER, request);

        assertTrue(result.locked());
        assertEquals(MEMBER, result.lockedBy());
        assertEquals(MEMBER_NAME, result.lockedByName());
    }

    @Test
    @DisplayName("회의록에 대해 타인이 이미 잠그고 있으면 acquire 시 예외를 던진다")
    public void acquire_MeetingRecord_Conflict_ThrowsException() {
        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(OTHER_MEMBER)
            .build());
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MEETING_RECORD)
            .targetId(MEETING_RECORD_ID)
            .sectionKey(SECTION_KEY)
            .build();
        facade.acquire(MEMBER, request);

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, request))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("회의록의 팀 소속이 아니면 getStatus 조회를 거부한다")
    public void getStatus_MeetingRecord_RejectsNonMember() {
        String outsider = "202400000";

        assertThatThrownBy(() -> facade.getStatus(EditLockTargetType.MEETING_RECORD, MEETING_RECORD_ID, SECTION_KEY, outsider))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("분반의 활성 학생이 아니면 회의록 잠금을 획득할 수 없다")
    public void acquire_MeetingRecord_RejectsInactiveStudent() {
        String assistant = "202488888";
        teamMemberRepository.save(TeamMember.create(TEAM_ID, assistant, false, "조교"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, assistant, Role.ASSISTANT, Status.ACTIVE));

        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MEETING_RECORD)
            .targetId(MEETING_RECORD_ID)
            .sectionKey(SECTION_KEY)
            .build();

        assertThatThrownBy(() -> facade.acquire(assistant, request))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("중간보고서에 대해 acquire는 잠금을 획득하고 lockedByName을 포함해 반환한다 (MID_REPORT)")
    public void acquire_MidReport_Success() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MID_REPORT)
            .targetId(MID_REPORT_ID)
            .sectionKey("topic")
            .build();

        EditLockStatusResponse result = facade.acquire(MEMBER, request);

        assertTrue(result.locked());
        assertEquals(MEMBER, result.lockedBy());
        assertEquals(MEMBER_NAME, result.lockedByName());
    }

    @Test
    @DisplayName("중간보고서에 대해 MID_REPORT_BLOCK 타입으로도 잠금을 획득할 수 있다")
    public void acquire_MidReportBlock_Success() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MID_REPORT_BLOCK)
            .targetId(MID_REPORT_ID)
            .sectionKey("gui-design")
            .build();

        EditLockStatusResponse result = facade.acquire(MEMBER, request);

        assertTrue(result.locked());
        assertEquals(MEMBER, result.lockedBy());
        assertEquals(MEMBER_NAME, result.lockedByName());
    }

    @Test
    @DisplayName("중간보고서에 대해 타인이 이미 동일 섹션을 잠그고 있으면 acquire 시 예외를 던진다")
    public void acquire_MidReport_Conflict_ThrowsException() {
        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(OTHER_MEMBER)
            .build());
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MID_REPORT)
            .targetId(MID_REPORT_ID)
            .sectionKey("topic")
            .build();
        facade.acquire(MEMBER, request);

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, request))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("중간보고서의 팀 소속이 아니면 getStatus 조회를 거부한다")
    public void getStatus_MidReport_RejectsNonMember() {
        String outsider = "202400000";

        assertThatThrownBy(() -> facade.getStatus(EditLockTargetType.MID_REPORT, MID_REPORT_ID, "topic", outsider))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("분반의 활성 학생이 아니면 중간보고서 잠금을 획득할 수 없다")
    public void acquire_MidReport_RejectsInactiveStudent() {
        String assistant = "202488888";
        teamMemberRepository.save(TeamMember.create(TEAM_ID, assistant, false, "조교"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, assistant, Role.ASSISTANT, Status.ACTIVE));

        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MID_REPORT)
            .targetId(MID_REPORT_ID)
            .sectionKey("topic")
            .build();

        assertThatThrownBy(() -> facade.acquire(assistant, request))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 중간보고서 ID로 잠금을 획득하려 하면 MidReportNotFoundException이 발생한다")
    public void acquire_MidReport_RejectsNonExistent() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.MID_REPORT)
            .targetId(999999L)
            .sectionKey("topic")
            .build();

        assertThatThrownBy(() -> facade.acquire(MEMBER, request))
            .isInstanceOf(MidReportNotFoundException.class);
    }
}
