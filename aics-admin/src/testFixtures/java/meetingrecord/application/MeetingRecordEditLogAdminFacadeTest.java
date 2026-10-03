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
import kgu.developers.admin.meetingrecord.application.MeetingRecordEditLogAdminFacade;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordEditLogQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
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
class MeetingRecordEditLogAdminFacadeTest {

    private static final String PROFESSOR_ID = "202699999";
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MeetingRecordQueryService meetingRecordQueryService;

    @Mock
    private MeetingRecordEditLogQueryService meetingRecordEditLogQueryService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private MeetingRecordEditLogAdminFacade meetingRecordEditLogAdminFacade;

    @Test
    @DisplayName("담당 교수는 분반 전체 팀의 수정 이력을 팀명·회의록 제목·수정자 이름과 함께 조회한다")
    void getSectionMeetingRecordLogs_OwnedSection() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, PROFESSOR_ID))));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team(10L, "1팀"), team(20L, "2팀")));
        given(meetingRecordEditLogQueryService.getSectionLogs(
            List.of(10L, 20L), null, latestFirst()))
            .willReturn(new PageImpl<>(List.of(editLog(3L, 7L, 20L, "202412345")), latestFirst(), 1));
        given(meetingRecordQueryService.getMeetingRecords(List.of(7L))).willReturn(List.of(meetingRecord(7L, 20L)));
        given(userQueryService.getUsersByStudentNumbers(List.of("202412345")))
            .willReturn(List.of(user("202412345", "홍길동")));

        var response = meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            1L, null, null, PAGEABLE, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.id()).isEqualTo(3L);
            assertThat(content.teamId()).isEqualTo(20L);
            assertThat(content.teamName()).isEqualTo("2팀");
            assertThat(content.meetingRecordId()).isEqualTo(7L);
            assertThat(content.meetingRecordTitle()).isEqualTo("3주차 정기 회의");
            assertThat(content.editorName()).isEqualTo("홍길동");
            assertThat(content.createdAt()).isEqualTo("2026-10-04 15:30");
        });
    }

    @Test
    @DisplayName("조회는 최신순으로 고정되며 클라이언트 정렬은 적용하지 않는다")
    void getSectionMeetingRecordLogs_FixedSort() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, PROFESSOR_ID))));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team(10L, "1팀")));
        given(meetingRecordEditLogQueryService.getSectionLogs(anyList(), any(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), latestFirst(), 0));

        meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            1L, null, null, PageRequest.of(0, 20, Sort.by("reason")), PROFESSOR_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordEditLogQueryService).getSectionLogs(anyList(), any(), captor.capture());
        assertThat(captor.getValue().getSort())
            .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    @DisplayName("팀 필터는 담당 분반 소속 팀으로 범위를 좁힌다")
    void getSectionMeetingRecordLogs_TeamFilter() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, PROFESSOR_ID))));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team(10L, "1팀"), team(20L, "2팀")));
        given(meetingRecordEditLogQueryService.getSectionLogs(List.of(20L), null, latestFirst()))
            .willReturn(new PageImpl<>(List.of(), latestFirst(), 0));

        meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(1L, 20L, null, PAGEABLE, PROFESSOR_ID);

        verify(meetingRecordEditLogQueryService).getSectionLogs(List.of(20L), null, latestFirst());
    }

    @Test
    @DisplayName("담당 분반 소속이 아닌 팀을 지정하면 403으로 막는다")
    void getSectionMeetingRecordLogs_ForeignTeam() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, PROFESSOR_ID))));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team(10L, "1팀")));

        assertThatThrownBy(() -> meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            1L, 99L, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 회의록 수정 이력만 조회할 수 있습니다.");

        verify(meetingRecordEditLogQueryService, never()).getSectionLogs(anyList(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("담당 분반 밖 회의록을 지정하면 403으로 막는다")
    void getSectionMeetingRecordLogs_ForeignMeetingRecord() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, PROFESSOR_ID))));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team(10L, "1팀")));
        given(meetingRecordQueryService.getMeetingRecords(List.of(7L))).willReturn(List.of(meetingRecord(7L, 99L)));

        assertThatThrownBy(() -> meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            1L, null, 7L, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 회의록 수정 이력만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 회의록도 존재 여부를 노출하지 않고 403으로 막는다")
    void getSectionMeetingRecordLogs_MeetingRecordNotFound() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, PROFESSOR_ID))));
        given(teamRepository.findAllBySectionId(1L)).willReturn(List.of(team(10L, "1팀")));
        given(meetingRecordQueryService.getMeetingRecords(List.of(7L))).willReturn(List.of());

        assertThatThrownBy(() -> meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            1L, null, 7L, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 회의록 수정 이력만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("다른 교수의 분반과 존재하지 않는 분반은 모두 403으로 막는다")
    void getSectionMeetingRecordLogs_ForeignOrMissingSection() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section(1L, "202688888"))));
        given(sectionRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            1L, null, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            2L, null, null, PAGEABLE, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(teamRepository, never()).findAllBySectionId(anyLong());
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

    private Team team(Long id, String name) {
        return Team.builder()
            .id(id)
            .sectionId(1L)
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
            .content("내용")
            .participants(List.of())
            .build();
    }

    private MeetingRecordEditLog editLog(Long id, Long meetingRecordId, Long teamId, String editorId) {
        return MeetingRecordEditLog.builder()
            .id(id)
            .meetingRecordId(meetingRecordId)
            .teamId(teamId)
            .editorId(editorId)
            .reason("회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 고쳤습니다.")
            .createdAt(LocalDateTime.of(2026, 10, 4, 15, 30))
            .build();
    }

    private User user(String studentNumber, String name) {
        return User.builder()
            .studentNumber(studentNumber)
            .name(name)
            .build();
    }
}
