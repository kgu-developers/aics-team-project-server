package meetingrecord.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.admin.meetingrecord.application.MeetingRecordAdminFacade;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MeetingRecordAdminFacadeTest {

    private static final String PROFESSOR_ID = "202699999";

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MeetingRecordQueryService meetingRecordQueryService;

    @InjectMocks
    private MeetingRecordAdminFacade meetingRecordAdminFacade;

    @Test
    @DisplayName("전체 조회는 담당 교수의 모든 분반 팀 회의록을 분반·팀 정보와 함께 응답한다")
    void getMeetingRecords_AllOwnedSections() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("authorId"));
        Pageable latestFirstPageable = latestFirst(pageable);
        Section firstSection = section(1L, "1151", PROFESSOR_ID);
        Section secondSection = section(2L, "1152", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 2L, "B팀");
        MeetingRecord meetingRecord = meetingRecord(100L, 20L, "와이어프레임 기획 논의");

        given(sectionRepository.findAllByProfessorId(PROFESSOR_ID))
            .willReturn(List.of(detail(firstSection), detail(secondSection)));
        given(teamRepository.findAllBySectionIdIn(List.of(1L, 2L)))
            .willReturn(List.of(firstTeam, secondTeam));
        given(meetingRecordQueryService.getMeetingRecords(List.of(10L, 20L), latestFirstPageable))
            .willReturn(new PageImpl<>(List.of(meetingRecord), latestFirstPageable, 1));

        var response = meetingRecordAdminFacade.getMeetingRecords(null, pageable, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.sectionId()).isEqualTo(2L);
            assertThat(content.sectionName()).isEqualTo("1152");
            assertThat(content.teamId()).isEqualTo(20L);
            assertThat(content.teamName()).isEqualTo("B팀");
            assertThat(content.content()).isEqualTo("와이어프레임 기획 논의");
        });
        assertThat(response.pageable().totalElements()).isEqualTo(1);
        verify(teamRepository).findAllBySectionIdIn(List.of(1L, 2L));
        verify(meetingRecordQueryService)
            .getMeetingRecords(List.of(10L, 20L), latestFirstPageable);
    }

    @Test
    @DisplayName("분반 조회는 요청한 담당 분반의 팀만 조회한다")
    void getMeetingRecords_OwnedSection() {
        Pageable pageable = PageRequest.of(0, 20);
        Pageable latestFirstPageable = latestFirst(pageable);
        Section section = section(1L, "1151", PROFESSOR_ID);
        Team team = team(10L, 1L, "A팀");

        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(section)));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(team));
        given(meetingRecordQueryService.getMeetingRecords(List.of(10L), latestFirstPageable))
            .willReturn(new PageImpl<>(List.of(), latestFirstPageable, 0));

        meetingRecordAdminFacade.getMeetingRecords(1L, pageable, PROFESSOR_ID);

        verify(meetingRecordQueryService).getMeetingRecords(List.of(10L), latestFirstPageable);
        verify(sectionRepository, never()).findAllByProfessorId(PROFESSOR_ID);
    }

    @Test
    @DisplayName("다른 교수의 분반은 조회할 수 없다")
    void getMeetingRecords_ForeignSection() {
        Pageable pageable = PageRequest.of(0, 20);
        Section foreignSection = section(1L, "1151", "202600001");
        given(sectionRepository.findById(1L)).willReturn(Optional.of(detail(foreignSection)));

        assertThatThrownBy(() -> meetingRecordAdminFacade.getMeetingRecords(1L, pageable, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 회의록만 조회할 수 있습니다.");

        verify(teamRepository, never()).findAllBySectionIdIn(anyList());
    }

    private Section section(Long id, String name, String professorId) {
        return Section.builder()
            .id(id)
            .professorId(professorId)
            .name(name)
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

    private MeetingRecord meetingRecord(Long id, Long teamId, String content) {
        return MeetingRecord.builder()
            .id(id)
            .teamId(teamId)
            .phase(MeetingPhase.MID_CHECK)
            .authorId("202612345")
            .meetingAt(LocalDateTime.of(2026, 8, 25, 19, 30))
            .content(content)
            .participants(List.of())
            .build();
    }

    private Pageable latestFirst(Pageable pageable) {
        return PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Order.desc("meetingAt"), Sort.Order.desc("id"))
        );
    }
}
