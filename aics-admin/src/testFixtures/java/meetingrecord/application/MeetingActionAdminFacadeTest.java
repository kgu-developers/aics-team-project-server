package meetingrecord.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.admin.meetingrecord.application.MeetingActionAdminFacade;
import kgu.developers.domain.meetingrecord.application.query.MeetingActionQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MeetingActionAdminFacadeTest {

    private static final String PROFESSOR_ID = "202699999";
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MeetingActionQueryService meetingActionQueryService;

    @Mock
    private MeetingRecordQueryService meetingRecordQueryService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private MeetingActionAdminFacade meetingActionAdminFacade;

    @Test
    @DisplayName("담당 교수는 분반 전체 팀의 액션플랜을 팀명·회의록 제목·담당자 이름과 함께 조회한다")
    void getSectionMeetingActions_OwnedSection() {
        Section section = section(1L, PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "1팀");
        Team secondTeam = team(20L, 1L, "2팀");
        MeetingRecord meetingRecord = meetingRecord(100L, 20L);
        MeetingAction meetingAction = meetingAction(5L, 100L, "202412345", MeetingActionStatus.IN_PROGRESS);

        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(firstTeam, secondTeam));
        given(meetingActionQueryService.getSectionActions(
            List.of(10L, 20L), null, null, latestFirst()))
            .willReturn(new PageImpl<>(List.of(meetingAction), latestFirst(), 1));
        given(meetingRecordQueryService.getMeetingRecords(List.of(100L))).willReturn(List.of(meetingRecord));
        given(userQueryService.getUsersByStudentNumbers(List.of("202412345")))
            .willReturn(List.of(user("202412345", "홍길동")));

        var response = meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, null, PAGEABLE, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.id()).isEqualTo(5L);
            assertThat(content.teamId()).isEqualTo(20L);
            assertThat(content.teamName()).isEqualTo("2팀");
            assertThat(content.meetingRecordId()).isEqualTo(100L);
            assertThat(content.meetingRecordTitle()).isEqualTo("3주차 정기 회의");
            assertThat(content.assigneeId()).isEqualTo("202412345");
            assertThat(content.assigneeName()).isEqualTo("홍길동");
            assertThat(content.status()).isEqualTo(MeetingActionStatus.IN_PROGRESS);
            assertThat(content.dueAt()).isEqualTo("2026-10-07 23:59");
        });
        assertThat(response.pageable().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("조회는 생성 최신순으로 고정되며 클라이언트 정렬은 적용하지 않는다")
    void getSectionMeetingActions_FixedSort() {
        Section section = section(1L, PROFESSOR_ID);
        Team team = team(10L, 1L, "1팀");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team));
        given(meetingActionQueryService.getSectionActions(anyList(), any(), any(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), latestFirst(), 0));

        meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, null, PageRequest.of(0, 20, Sort.by("content")), PROFESSOR_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingActionQueryService).getSectionActions(anyList(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort())
            .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    @DisplayName("팀 필터는 담당 분반 소속 팀으로 범위를 좁힌다")
    void getSectionMeetingActions_TeamFilter() {
        Section section = section(1L, PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "1팀");
        Team secondTeam = team(20L, 1L, "2팀");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(firstTeam, secondTeam));
        given(meetingActionQueryService.getSectionActions(List.of(20L), null, null, latestFirst()))
            .willReturn(new PageImpl<>(List.of(), latestFirst(), 0));

        meetingActionAdminFacade.getSectionMeetingActions(1L, 20L, null, null, PAGEABLE, PROFESSOR_ID);

        verify(meetingActionQueryService).getSectionActions(List.of(20L), null, null, latestFirst());
    }

    @Test
    @DisplayName("상태 필터를 그대로 조회 조건으로 넘긴다")
    void getSectionMeetingActions_StatusFilter() {
        Section section = section(1L, PROFESSOR_ID);
        Team team = team(10L, 1L, "1팀");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team));
        given(meetingActionQueryService.getSectionActions(
            List.of(10L), null, MeetingActionStatus.DONE, latestFirst()))
            .willReturn(new PageImpl<>(List.of(), latestFirst(), 0));

        meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, MeetingActionStatus.DONE, PAGEABLE, PROFESSOR_ID);

        verify(meetingActionQueryService)
            .getSectionActions(List.of(10L), null, MeetingActionStatus.DONE, latestFirst());
    }

    @Test
    @DisplayName("담당 분반 소속이 아닌 팀을 지정하면 403으로 막는다")
    void getSectionMeetingActions_ForeignTeam() {
        Section section = section(1L, PROFESSOR_ID);
        Team team = team(10L, 1L, "1팀");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team));

        assertThatThrownBy(() -> meetingActionAdminFacade.getSectionMeetingActions(
            1L, 99L, null, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 액션플랜만 조회할 수 있습니다.");

        verify(meetingActionQueryService, never()).getSectionActions(anyList(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("담당 분반 밖 회의록을 지정하면 403으로 막는다")
    void getSectionMeetingActions_ForeignMeetingRecord() {
        Section section = section(1L, PROFESSOR_ID);
        Team team = team(10L, 1L, "1팀");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team));
        given(meetingRecordQueryService.getMeetingRecords(List.of(100L)))
            .willReturn(List.of(meetingRecord(100L, 999L)));

        assertThatThrownBy(() -> meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, 100L, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 액션플랜만 조회할 수 있습니다.");

        verify(meetingActionQueryService, never()).getSectionActions(anyList(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("존재하지 않는 회의록도 존재 여부를 노출하지 않고 403으로 막는다")
    void getSectionMeetingActions_MeetingRecordNotFound() {
        Section section = section(1L, PROFESSOR_ID);
        Team team = team(10L, 1L, "1팀");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team));
        given(meetingRecordQueryService.getMeetingRecords(List.of(100L))).willReturn(List.of());

        assertThatThrownBy(() -> meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, 100L, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 액션플랜만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("다른 교수의 분반은 조회할 수 없다")
    void getSectionMeetingActions_ForeignSection() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, "202688888"))));

        assertThatThrownBy(() -> meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 액션플랜만 조회할 수 있습니다.");

        verify(teamRepository, never()).findAllBySectionId(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 분반도 존재 여부를 노출하지 않고 403으로 막는다")
    void getSectionMeetingActions_SectionNotFound() {
        given(sectionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 액션플랜만 조회할 수 있습니다.");

        verify(teamRepository, never()).findAllBySectionId(anyLong());
    }

    @Test
    @DisplayName("담당자가 없는 액션플랜은 담당자 조회 없이 null로 응답한다")
    void getSectionMeetingActions_WithoutAssignee() {
        Section section = section(1L, PROFESSOR_ID);
        Team team = team(10L, 1L, "1팀");
        MeetingRecord meetingRecord = meetingRecord(100L, 10L);
        MeetingAction meetingAction = meetingAction(5L, 100L, null, MeetingActionStatus.TODO);

        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team));
        given(meetingActionQueryService.getSectionActions(List.of(10L), null, null, latestFirst()))
            .willReturn(new PageImpl<>(List.of(meetingAction), latestFirst(), 1));
        given(meetingRecordQueryService.getMeetingRecords(List.of(100L))).willReturn(List.of(meetingRecord));

        var response = meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, null, PAGEABLE, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.assigneeId()).isNull();
            assertThat(content.assigneeName()).isNull();
        });
        verify(userQueryService, never()).getUsersByStudentNumbers(anyList());
    }

    @Test
    @DisplayName("팀이 없는 분반은 빈 목록으로 응답한다")
    void getSectionMeetingActions_EmptySection() {
        Section section = section(1L, PROFESSOR_ID);
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of());
        given(meetingActionQueryService.getSectionActions(List.of(), null, null, latestFirst()))
            .willReturn(new PageImpl<>(List.of(), latestFirst(), 0));

        var response = meetingActionAdminFacade.getSectionMeetingActions(
            1L, null, null, null, PAGEABLE, PROFESSOR_ID);

        assertThat(response.contents()).isEmpty();
        assertThat(response.pageable().totalElements()).isZero();
    }

    private Pageable latestFirst() {
        return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private Section section(Long id, String professorId) {
        return Section.builder()
            .id(id)
            .professorId(professorId)
            .classTime("월3,4")
            .code("1151")
            .build();
    }

    private SectionDetail detail(Section section) {
        return new SectionDetail(section, null, null);
    }

    private Team team(Long id, Long sectionId, String name) {
        return Team.builder()
            .id(id)
            .sectionId(sectionId)
            .name(name)
            .build();
    }

    private MeetingRecord meetingRecord(Long id, Long teamId) {
        return MeetingRecord.builder()
            .id(id)
            .teamId(teamId)
            .title("3주차 정기 회의")
            .phase(MeetingPhase.MID_CHECK)
            .authorId("202612345")
            .meetingAt(LocalDateTime.of(2026, 9, 30, 14, 0))
            .content("와이어프레임 기획 논의")
            .participants(List.of())
            .createdAt(LocalDateTime.of(2026, 9, 30, 14, 0))
            .updatedAt(LocalDateTime.of(2026, 9, 30, 15, 0))
            .build();
    }

    private MeetingAction meetingAction(Long id, Long meetingRecordId, String assigneeId, MeetingActionStatus status) {
        return MeetingAction.builder()
            .id(id)
            .meetingRecordId(meetingRecordId)
            .assigneeId(assigneeId)
            .content("로그인 API 연동")
            .status(status)
            .dueAt(LocalDateTime.of(2026, 10, 7, 23, 59))
            .createdAt(LocalDateTime.of(2026, 9, 30, 15, 0))
            .updatedAt(LocalDateTime.of(2026, 10, 1, 9, 30))
            .build();
    }

    private User user(String studentNumber, String name) {
        return User.builder()
            .studentNumber(studentNumber)
            .name(name)
            .build();
    }
}
