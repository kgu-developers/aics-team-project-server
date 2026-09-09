package meetingrecord.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.api.meetingrecord.application.MeetingRecordFacade;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.meetingrecord.application.command.MeetingRecordCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import mock.repository.FakeMeetingRecordRepository;
import mock.repository.FakeTeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

public class MeetingRecordFacadeTest {

    private static final String MEMBER = "202412345";
    private static final String NON_MEMBER = "202400000";

    private MeetingRecordFacade meetingRecordFacade;
    private TeamRepository teamRepository;
    private MilestoneRepository milestoneRepository;

    @BeforeEach
    public void init() {
        FakeMeetingRecordRepository fakeMeetingRecordRepository = new FakeMeetingRecordRepository();
        FakeTeamMemberRepository fakeTeamMemberRepository = new FakeTeamMemberRepository();
        fakeTeamMemberRepository.save(TeamMember.create(1L, MEMBER, false, "기록자"));

        teamRepository = mock(TeamRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        meetingRecordFacade = new MeetingRecordFacade(
            new MeetingRecordCommandService(fakeMeetingRecordRepository),
            new MeetingRecordQueryService(fakeMeetingRecordRepository),
            fakeTeamMemberRepository,
            teamRepository,
            milestoneRepository
        );
    }

    @Test
    @DisplayName("createMeetingRecord는 참석자를 포함한 회의록을 생성한다")
    public void createMeetingRecord_Success() {
        // given
        MeetingRecordCreateRequest request = buildCreateRequest(MeetingPhase.MID_CHECK, List.of("202412345", "202412346"));

        // when
        MeetingRecordPersistResponse result = meetingRecordFacade.createMeetingRecord(1L, "202412345", request);

        // then
        assertNotNull(result.id());
        assertEquals(MeetingPhase.MID_CHECK, result.phase());
        assertEquals("202412345", result.authorId());
    }

    @Test
    @DisplayName("createMeetingRecord는 중복된 참석자 학번을 제거하여 저장한다")
    public void createMeetingRecord_DedupesParticipants() {
        // given
        MeetingRecordCreateRequest request = buildCreateRequest(
            MeetingPhase.PROPOSAL,
            List.of("202412345", "202412345", "202412346")
        );

        // when
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(1L, "202412345", request);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id(), MEMBER);

        // then
        assertEquals(2, detail.participantIds().size());
    }

    @Test
    @DisplayName("회의록은 같은 분반의 관련 마일스톤을 중복 없이 연결하고 목록·상세에 요약을 응답한다")
    void meetingRecord_LinksMilestonesInSameSection() {
        Team team = Team.builder().id(1L).sectionId(10L).name("A팀").build();
        Milestone milestone = mock(Milestone.class);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(milestoneRepository.findById(3L)).willReturn(Optional.of(milestone));
        given(milestone.belongsToSection(10L)).willReturn(true);
        given(milestone.getId()).willReturn(3L);
        given(milestone.getTitle()).willReturn("중간 보고서");
        given(milestone.getType()).willReturn(MilestoneType.MID_REPORT);
        given(milestone.getWeekNumber()).willReturn(8);

        MeetingRecordCreateRequest request = MeetingRecordCreateRequest.builder()
            .title("3주차 정기 회의")
            .meetingAt(LocalDateTime.of(2026, 8, 3, 14, 0))
            .phase(MeetingPhase.MID_CHECK)
            .content("회의 내용")
            .participantIds(List.of(MEMBER))
            .milestoneIds(List.of(3L, 3L))
            .build();

        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(1L, MEMBER, request);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id(), MEMBER);
        MeetingRecordListResponse list = meetingRecordFacade.getMeetingRecords(1L, null, MEMBER);

        assertEquals(1, detail.milestones().size());
        assertEquals("중간 보고서", detail.milestones().get(0).title());
        assertEquals(1, list.contents().get(0).milestones().size());
    }

    @Test
    @DisplayName("다른 분반 마일스톤은 회의록에 연결할 수 없다")
    void createMeetingRecord_RejectsMilestoneFromOtherSection() {
        Team team = Team.builder().id(1L).sectionId(10L).name("A팀").build();
        Milestone milestone = mock(Milestone.class);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(milestoneRepository.findById(3L)).willReturn(Optional.of(milestone));
        given(milestone.belongsToSection(10L)).willReturn(false);

        MeetingRecordCreateRequest request = MeetingRecordCreateRequest.builder()
            .title("3주차 정기 회의")
            .meetingAt(LocalDateTime.of(2026, 8, 3, 14, 0))
            .phase(MeetingPhase.MID_CHECK)
            .content("회의 내용")
            .milestoneIds(List.of(3L))
            .build();

        assertThatThrownBy(() -> meetingRecordFacade.createMeetingRecord(1L, MEMBER, request))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getMeetingRecords는 phase로 필터링한 목록을 반환한다")
    public void getMeetingRecords_FilterByPhase() {
        // given
        meetingRecordFacade.createMeetingRecord(1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345")));
        meetingRecordFacade.createMeetingRecord(1L, "202412345", buildCreateRequest(MeetingPhase.FINAL, List.of("202412345")));

        // when
        MeetingRecordListResponse result = meetingRecordFacade.getMeetingRecords(1L, MeetingPhase.FINAL, MEMBER);

        // then
        assertEquals(1, result.contents().size());
        assertEquals(MeetingPhase.FINAL, result.contents().get(0).phase());
    }

    @Test
    @DisplayName("getMeetingRecord는 존재하지 않는 id면 예외를 던진다")
    public void getMeetingRecord_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> meetingRecordFacade.getMeetingRecord(999L, MEMBER))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("소속되지 않은 팀의 회의록 목록/상세를 조회하면 접근이 거부된다")
    public void getMeetingRecords_NonMember_ThrowsAccessDenied() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345"))
        );

        // when & then
        assertThatThrownBy(() -> meetingRecordFacade.getMeetingRecords(1L, null, NON_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> meetingRecordFacade.getMeetingRecord(persisted.id(), NON_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("소속되지 않은 팀에는 회의록을 생성할 수 없다")
    public void createMeetingRecord_NonMember_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> meetingRecordFacade.createMeetingRecord(
            1L, NON_MEMBER, buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345"))
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateMeetingRecord는 participantIds가 주어지면 참석자 목록 전체를 치환한다")
    public void updateMeetingRecord_ReplacesParticipants() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345", "202412346"))
        );
        MeetingRecordUpdateRequest updateRequest = MeetingRecordUpdateRequest.builder()
            .participantIds(List.of("202499999"))
            .build();

        // when
        meetingRecordFacade.updateMeetingRecord(persisted.id(), updateRequest, MEMBER);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id(), MEMBER);

        // then
        assertEquals(List.of("202499999"), detail.participantIds());
    }

    @Test
    @DisplayName("updateMeetingRecord는 공백만 있는 content로 수정하면 예외를 던진다")
    public void updateMeetingRecord_BlankContent_ThrowsException() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345"))
        );
        MeetingRecordUpdateRequest updateRequest = MeetingRecordUpdateRequest.builder()
            .content("   ")
            .build();

        // when & then
        assertThatThrownBy(() -> meetingRecordFacade.updateMeetingRecord(persisted.id(), updateRequest, MEMBER))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateMeetingRecord는 participantIds가 주어지지 않으면 기존 참석자를 유지한다")
    public void updateMeetingRecord_KeepsParticipants_WhenNotProvided() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345", "202412346"))
        );
        MeetingRecordUpdateRequest updateRequest = MeetingRecordUpdateRequest.builder()
            .content("수정된 내용")
            .build();

        // when
        meetingRecordFacade.updateMeetingRecord(persisted.id(), updateRequest, MEMBER);
        MeetingRecordDetailResponse detail = meetingRecordFacade.getMeetingRecord(persisted.id(), MEMBER);

        // then
        assertEquals(2, detail.participantIds().size());
        assertEquals("수정된 내용", detail.content());
    }

    @Test
    @DisplayName("deleteMeetingRecord는 회의록을 삭제한다")
    public void deleteMeetingRecord_Success() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345"))
        );

        // when
        meetingRecordFacade.deleteMeetingRecord(persisted.id(), MEMBER);

        // then
        assertThatThrownBy(() -> meetingRecordFacade.getMeetingRecord(persisted.id(), MEMBER))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("소속되지 않은 팀의 회의록은 수정/삭제할 수 없다")
    public void updateAndDeleteMeetingRecord_NonMember_ThrowsAccessDenied() {
        // given
        MeetingRecordPersistResponse persisted = meetingRecordFacade.createMeetingRecord(
            1L, "202412345", buildCreateRequest(MeetingPhase.PROPOSAL, List.of("202412345"))
        );
        MeetingRecordUpdateRequest updateRequest = MeetingRecordUpdateRequest.builder()
            .content("수정 시도")
            .build();

        // when & then
        assertThatThrownBy(() -> meetingRecordFacade.updateMeetingRecord(persisted.id(), updateRequest, NON_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> meetingRecordFacade.deleteMeetingRecord(persisted.id(), NON_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
    }

    private MeetingRecordCreateRequest buildCreateRequest(MeetingPhase phase, List<String> participantIds) {
        return MeetingRecordCreateRequest.builder()
            .meetingAt(LocalDateTime.of(2026, 8, 3, 14, 0))
            .location("온라인(Zoom)")
            .phase(phase)
            .content("회의 내용")
            .participantIds(participantIds)
            .build();
    }
}
