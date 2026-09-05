package teammessage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kgu.developers.admin.teammessage.application.TeamMessageAdminFacade;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
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
class TeamMessageAdminFacadeTest {

    private static final String PROFESSOR_ID = "202699999";

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionQueryService sectionQueryService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamThreadQueryService teamThreadQueryService;

    @Mock
    private TeamMessageCommandService teamMessageCommandService;

    @Mock
    private TeamMessageQueryService teamMessageQueryService;

    @InjectMocks
    private TeamMessageAdminFacade teamMessageAdminFacade;

    @Test
    @DisplayName("전체 조회는 담당 교수의 팀 메시지를 분반·팀·읽음 정보와 함께 응답한다")
    void getMessages_AllOwnedSections() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("senderId"));
        Pageable latestFirstPageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("id"))
        );
        Section firstSection = section(1L, "1151", PROFESSOR_ID);
        Section secondSection = section(2L, "1152", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 2L, "B팀");
        TeamThread firstThread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamThread secondThread = TeamThread.builder().id(200L).teamId(20L).build();
        TeamMessage message = message(1000L, 200L, "화면설계서 확인 부탁드립니다.");

        given(sectionRepository.findAllByProfessorId(PROFESSOR_ID))
            .willReturn(List.of(detail(firstSection), detail(secondSection)));
        given(teamRepository.findAllBySectionIdIn(List.of(1L, 2L)))
            .willReturn(List.of(firstTeam, secondTeam));
        given(teamThreadQueryService.getThreads(List.of(10L, 20L)))
            .willReturn(List.of(firstThread, secondThread));
        given(teamMessageQueryService.getMessages(List.of(100L, 200L), latestFirstPageable))
            .willReturn(new PageImpl<>(List.of(message), latestFirstPageable, 1));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1000L))).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L, 200L), PROFESSOR_ID)).willReturn(3L);

        var response = teamMessageAdminFacade.getMessages(null, pageable, PROFESSOR_ID);

        assertThat(response.unreadCount()).isEqualTo(3L);
        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.sectionName()).isEqualTo("1152");
            assertThat(content.teamName()).isEqualTo("B팀");
            assertThat(content.message()).isEqualTo("화면설계서 확인 부탁드립니다.");
            assertThat(content.read()).isFalse();
        });
        verify(teamRepository).findAllBySectionIdIn(List.of(1L, 2L));
        verify(teamMessageQueryService).getMessages(List.of(100L, 200L), latestFirstPageable);
    }

    @Test
    @DisplayName("담당 분반을 지정하면 해당 분반 메시지만 조회한다")
    void getMessages_OwnedSection() {
        Pageable pageable = PageRequest.of(0, 20);
        Pageable latestFirstPageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("id"))
        );
        Section section = section(1L, "1151", PROFESSOR_ID);

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of());
        given(teamThreadQueryService.getThreads(List.of())).willReturn(List.of());
        given(teamMessageQueryService.getMessages(List.of(), latestFirstPageable))
            .willReturn(new PageImpl<>(List.of(), latestFirstPageable, 0));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of())).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(), PROFESSOR_ID)).willReturn(0L);

        var response = teamMessageAdminFacade.getMessages(1L, pageable, PROFESSOR_ID);

        assertThat(response.contents()).isEmpty();
        assertThat(response.unreadCount()).isZero();
        verify(teamRepository).findAllBySectionIdIn(List.of(1L));
    }

    @Test
    @DisplayName("다른 교수의 분반 메시지는 조회할 수 없다")
    void getMessages_ForeignSection() {
        Pageable pageable = PageRequest.of(0, 20);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> teamMessageAdminFacade.getMessages(1L, pageable, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 메시지만 조회할 수 있습니다.");

        verify(teamRepository, never()).findAllBySectionIdIn(anyList());
    }

    @Test
    @DisplayName("담당 교수는 본인 분반의 메시지를 읽음 처리한다")
    void markAsRead_OwnedSection() {
        TeamMessage message = message(1000L, 100L, "확인했습니다.");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();
        Team team = team(10L, 1L, "A팀");
        given(teamMessageQueryService.getMessage(1000L)).willReturn(message);
        given(teamThreadQueryService.getThreadById(100L)).willReturn(thread);
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);

        teamMessageAdminFacade.markAsRead(1000L, PROFESSOR_ID);

        verify(teamMessageCommandService).markAsRead(1000L, PROFESSOR_ID);
    }

    @Test
    @DisplayName("다른 교수 분반의 메시지는 읽음 처리할 수 없다")
    void markAsRead_ForeignSection() {
        TeamMessage message = message(1000L, 100L, "확인했습니다.");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();
        Team team = team(10L, 1L, "A팀");
        given(teamMessageQueryService.getMessage(1000L)).willReturn(message);
        given(teamThreadQueryService.getThreadById(100L)).willReturn(thread);
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> teamMessageAdminFacade.markAsRead(1000L, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 메시지만 읽음 처리할 수 있습니다.");

        verifyNoInteractions(teamMessageCommandService);
    }

    @Test
    @DisplayName("메시지가 참조한 팀이 없으면 팀 조회 오류로 처리한다")
    void markAsRead_TeamNotFound() {
        TeamMessage message = message(1000L, 100L, "확인했습니다.");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();
        given(teamMessageQueryService.getMessage(1000L)).willReturn(message);
        given(teamThreadQueryService.getThreadById(100L)).willReturn(thread);
        given(teamRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamMessageAdminFacade.markAsRead(1000L, PROFESSOR_ID))
            .isInstanceOf(TeamNotFoundException.class);

        verifyNoInteractions(teamMessageCommandService);
    }

    private Section section(Long id, String name, String professorId) {
        return Section.builder().id(id).name(name).professorId(professorId).build();
    }

    private SectionDetail detail(Section section) {
        return new SectionDetail(section, null, null);
    }

    private Team team(Long id, Long sectionId, String name) {
        return Team.builder().id(id).sectionId(sectionId).name(name).build();
    }

    private TeamMessage message(Long id, Long threadId, String content) {
        return TeamMessage.builder()
            .id(id)
            .threadId(threadId)
            .senderId("202612345")
            .message(content)
            .relatedType(TeamMessageRelatedType.GENERAL)
            .createdAt(LocalDateTime.of(2026, 8, 25, 19, 30))
            .build();
    }
}
