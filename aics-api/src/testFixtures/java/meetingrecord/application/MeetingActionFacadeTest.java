package meetingrecord.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.meetingrecord.application.MeetingActionFacade;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionResponse;
import kgu.developers.api.meetingrecord.presentation.response.TeamMeetingActionListResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.command.MeetingActionCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingActionQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeMeetingActionRepository;
import mock.repository.FakeMeetingRecordRepository;
import mock.repository.FakeSectionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

public class MeetingActionFacadeTest {

    private static final String MEMBER = "202412345";
    private static final String NON_MEMBER = "202400000";
    private static final String OTHER_TEAM_STUDENT = "202400111";
    private static final String PROFESSOR = "P0001";

    private MeetingActionFacade meetingActionFacade;
    private Long meetingRecordId;

    @BeforeEach
    public void init() {
        FakeMeetingRecordRepository fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        FakeMeetingActionRepository fakeMeetingActionRepository = new FakeMeetingActionRepository(fakeMeetingRecordRepository);
        FakeTeamMemberRepository fakeTeamMemberRepository = new FakeTeamMemberRepository();
        FakeTeamRepository fakeTeamRepository = new FakeTeamRepository();
        FakeSectionRepository fakeSectionRepository = new FakeSectionRepository();
        FakeUserRepository fakeUserRepository = new FakeUserRepository();
        fakeTeamMemberRepository.save(TeamMember.create(1L, MEMBER, false, "기록자"));
        fakeSectionRepository.save(Section.builder().id(10L).professorId(PROFESSOR).build());
        fakeTeamRepository.save(Team.builder().id(1L).sectionId(10L).status(Status.CONFIRMED).build());
        fakeUserRepository.save(User.create(MEMBER, "member@kyonggi.ac.kr", "회원", "pw", UserGlobalRole.USER, "010-0000-0000"));

        MeetingRecord meetingRecord = fakeMeetingRecordRepository.save(
            MeetingRecord.create(1L, "회의록 제목", MeetingPhase.PROPOSAL, MEMBER, LocalDateTime.now(), "장소", "내용", List.of(MEMBER))
        );
        meetingRecordId = meetingRecord.getId();

        meetingActionFacade = new MeetingActionFacade(
            new MeetingActionCommandService(fakeMeetingActionRepository),
            new MeetingActionQueryService(fakeMeetingActionRepository),
            new MeetingRecordQueryService(fakeMeetingRecordRepository),
            fakeTeamMemberRepository,
            new TeamAccessValidator(fakeTeamRepository, fakeTeamMemberRepository, fakeSectionRepository),
            new UserQueryService(fakeUserRepository, new FakeEnrollmentRepository())
        );
    }

    private MeetingActionCreateRequest buildCreateRequest() {
        return MeetingActionCreateRequest.builder()
            .content("작업 내용")
            .assigneeId(MEMBER)
            .build();
    }

    @Test
    @DisplayName("createMeetingAction은 액션플랜을 생성한다")
    public void createMeetingAction_Success() {
        // when
        MeetingActionResponse result = meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());

        // then
        assertNotNull(result.id());
        assertEquals("작업 내용", result.content());
        assertEquals(MeetingActionStatus.TODO, result.status());
    }

    @Test
    @DisplayName("소속되지 않은 팀의 회의록에는 액션플랜을 생성할 수 없다")
    public void createMeetingAction_NonMember_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> meetingActionFacade.createMeetingAction(meetingRecordId, NON_MEMBER, buildCreateRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("같은 팀 소속이 아닌 사용자를 담당자로 지정하면 예외를 던진다")
    public void createMeetingAction_AssigneeNotTeamMember_ThrowsException() {
        // given
        MeetingActionCreateRequest request = MeetingActionCreateRequest.builder()
            .content("작업 내용")
            .assigneeId(OTHER_TEAM_STUDENT)
            .build();

        // when & then
        assertThatThrownBy(() -> meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, request))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getMeetingActions는 회의록의 액션플랜 목록을 반환한다")
    public void getMeetingActions_Success() {
        // given
        meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());

        // when
        MeetingActionListResponse result = meetingActionFacade.getMeetingActions(meetingRecordId, MEMBER);

        // then
        assertEquals(1, result.contents().size());
    }

    @Test
    @DisplayName("소속되지 않은 팀의 액션플랜 목록은 조회할 수 없다")
    public void getMeetingActions_NonMember_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> meetingActionFacade.getMeetingActions(meetingRecordId, NON_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getMeetingActions는 담당 교수도 조회할 수 있다")
    public void getMeetingActions_Professor_Allowed() {
        // given
        meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());

        // when
        MeetingActionListResponse result = meetingActionFacade.getMeetingActions(meetingRecordId, PROFESSOR);

        // then
        assertEquals(1, result.contents().size());
    }

    @Test
    @DisplayName("updateMeetingAction은 전달된 필드만 갱신한다")
    public void updateMeetingAction_UpdatesOnlyProvidedFields() {
        // given
        MeetingActionResponse persisted = meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());
        MeetingActionUpdateRequest updateRequest = MeetingActionUpdateRequest.builder()
            .status(MeetingActionStatus.DONE)
            .build();

        // when
        MeetingActionResponse updated = meetingActionFacade.updateMeetingAction(persisted.id(), MEMBER, updateRequest);

        // then
        assertEquals(MeetingActionStatus.DONE, updated.status());
        assertEquals("작업 내용", updated.content());
    }

    @Test
    @DisplayName("updateMeetingAction은 clearAssignee가 true면 담당자를 해제한다")
    public void updateMeetingAction_ClearAssignee_RemovesAssignee() {
        // given
        MeetingActionResponse persisted = meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());
        MeetingActionUpdateRequest updateRequest = MeetingActionUpdateRequest.builder()
            .clearAssignee(true)
            .build();

        // when
        MeetingActionResponse updated = meetingActionFacade.updateMeetingAction(persisted.id(), MEMBER, updateRequest);

        // then
        assertEquals(null, updated.assignee());
    }

    @Test
    @DisplayName("같은 팀 소속이 아닌 사용자로 담당자를 변경하면 예외를 던진다")
    public void updateMeetingAction_AssigneeNotTeamMember_ThrowsException() {
        // given
        MeetingActionResponse persisted = meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());
        MeetingActionUpdateRequest updateRequest = MeetingActionUpdateRequest.builder()
            .assigneeId(OTHER_TEAM_STUDENT)
            .build();

        // when & then
        assertThatThrownBy(() -> meetingActionFacade.updateMeetingAction(persisted.id(), MEMBER, updateRequest))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("소속되지 않은 팀의 액션플랜은 수정할 수 없다")
    public void updateMeetingAction_NonMember_ThrowsAccessDenied() {
        // given
        MeetingActionResponse persisted = meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());
        MeetingActionUpdateRequest updateRequest = MeetingActionUpdateRequest.builder()
            .status(MeetingActionStatus.DONE)
            .build();

        // when & then
        assertThatThrownBy(() -> meetingActionFacade.updateMeetingAction(persisted.id(), NON_MEMBER, updateRequest))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateMeetingAction은 존재하지 않는 액션플랜이면 예외를 던진다")
    public void updateMeetingAction_NotFound_ThrowsException() {
        // given
        MeetingActionUpdateRequest updateRequest = MeetingActionUpdateRequest.builder()
            .status(MeetingActionStatus.DONE)
            .build();

        // when & then
        assertThatThrownBy(() -> meetingActionFacade.updateMeetingAction(999L, MEMBER, updateRequest))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getTeamActions는 팀 전체 액션플랜을 반환한다")
    public void getTeamActions_Success() {
        // given
        meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());

        // when
        TeamMeetingActionListResponse result = meetingActionFacade.getTeamActions(1L, null,MEMBER);

        // then
        assertEquals(1, result.contents().size());
    }

    @Test
    @DisplayName("소속되지 않은 팀의 전체 액션플랜은 조회할 수 없다")
    public void getTeamActions_NonMember_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> meetingActionFacade.getTeamActions(1L, null, NON_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getTeamActions는 담당 교수도 조회할 수 있다")
    public void getTeamActions_Professor_Allowed() {
        // given
        meetingActionFacade.createMeetingAction(meetingRecordId, MEMBER, buildCreateRequest());

        // when
        TeamMeetingActionListResponse result = meetingActionFacade.getTeamActions(1L, null,PROFESSOR);

        // then
        assertEquals(1, result.contents().size());
    }
}
