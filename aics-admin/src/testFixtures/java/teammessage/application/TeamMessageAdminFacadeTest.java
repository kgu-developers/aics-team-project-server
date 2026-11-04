package teammessage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
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
    private static final Pageable FETCH_ALL_PAGEABLE =
        PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Order.desc("id")));

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

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private TeamMemberQueryService teamMemberQueryService;

    @InjectMocks
    private TeamMessageAdminFacade teamMessageAdminFacade;

    @Test
    @DisplayName("전체 조회는 담당 교수의 팀 메시지를 분반·팀·읽음 정보와 함께 응답한다")
    void getMessages_AllOwnedSections() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("senderId"));
        Section firstSection = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Section secondSection = section(2L, "월3,4", "1152", PROFESSOR_ID);
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
        given(teamMessageQueryService.getMessages(List.of(100L, 200L), null, FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(message), FETCH_ALL_PAGEABLE, 1));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1000L))).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L, 200L), PROFESSOR_ID)).willReturn(3L);
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of("202612345")))
            .willReturn(List.of(User.create("202612345", "kgu@kyonggi.ac.kr", "김학생", "encoded",
                UserGlobalRole.USER, "010-1234-5678")));
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L, 20L)))
            .willReturn(java.util.Map.of(20L, "이영희"));

        var response = teamMessageAdminFacade.getMessages(null, pageable, PROFESSOR_ID);

        assertThat(response.unreadCount()).isEqualTo(3L);
        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.sectionName()).isEqualTo("월3,4/1152");
            assertThat(content.teamName()).isEqualTo("B팀");
            assertThat(content.message()).isEqualTo("화면설계서 확인 부탁드립니다.");
            assertThat(content.read()).isFalse();
            assertThat(content.senderName()).isEqualTo("김학생");
            assertThat(content.targetTeamCount()).isEqualTo(1);
            assertThat(content.leaderName()).isEqualTo("이영희");
        });
        verify(teamRepository).findAllBySectionIdIn(List.of(1L, 2L));
        verify(teamMessageQueryService).getMessages(List.of(100L, 200L), null, FETCH_ALL_PAGEABLE);
    }

    @Test
    @DisplayName("담당 분반을 지정하면 해당 분반 메시지만 조회한다")
    void getMessages_OwnedSection() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of());
        given(teamThreadQueryService.getThreads(List.of())).willReturn(List.of());
        given(teamMessageQueryService.getMessages(List.of(), null, FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(), FETCH_ALL_PAGEABLE, 0));
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
    @DisplayName("teamId와 relatedType을 지정하면 해당 팀 및 유형의 메시지만 조회한다")
    void getMessages_WithTeamIdAndRelatedType() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 1L, "B팀");
        TeamThread firstThread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamMessage message = message(1000L, 100L, "중간보고서 피드백입니다.");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L)))
            .willReturn(List.of(firstTeam, secondTeam));
        given(teamThreadQueryService.getThreads(List.of(10L)))
            .willReturn(List.of(firstThread));
        given(teamMessageQueryService.getMessages(List.of(100L), TeamMessageRelatedType.MID_REPORT, FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(message), FETCH_ALL_PAGEABLE, 1));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1000L))).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L), PROFESSOR_ID)).willReturn(1L);
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L))).willReturn(java.util.Map.of());

        var response = teamMessageAdminFacade.getMessages(
            1L, 10L, TeamMessageRelatedType.MID_REPORT, null, Sort.Direction.DESC, pageable, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.teamName()).isEqualTo("A팀");
            assertThat(content.message()).isEqualTo("중간보고서 피드백입니다.");
        });
        verify(teamMessageQueryService).getMessages(List.of(100L), TeamMessageRelatedType.MID_REPORT, FETCH_ALL_PAGEABLE);
    }

    @Test
    @DisplayName("같은 broadcastId를 가진 메시지는 한 묶음으로 보여주고 대상 팀 수를 포함한다")
    void getMessages_GroupsBroadcastMessages() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 1L, "B팀");
        TeamThread firstThread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamThread secondThread = TeamThread.builder().id(200L).teamId(20L).build();
        TeamMessage first = message(1001L, 100L, "전체 공지", "broadcast-1");
        TeamMessage second = message(1002L, 200L, "전체 공지", "broadcast-1");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(firstTeam, secondTeam));
        given(teamThreadQueryService.getThreads(List.of(10L, 20L))).willReturn(List.of(firstThread, secondThread));
        given(teamMessageQueryService.getMessages(List.of(100L, 200L), null, FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(first, second), FETCH_ALL_PAGEABLE, 2));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1001L))).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L, 200L), PROFESSOR_ID)).willReturn(1L);
        given(teamMessageQueryService.getMessagesByBroadcastId("broadcast-1"))
            .willReturn(List.of(first, second));
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L, 20L)))
            .willReturn(java.util.Map.of(10L, "김팀장"));

        var response = teamMessageAdminFacade.getMessages(1L, pageable, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.id()).isEqualTo(1001L);
            assertThat(content.targetTeamCount()).isEqualTo(2);
            assertThat(content.leaderName()).isEqualTo("김팀장");
        });
        assertThat(response.pageable().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("오름차순 조회에서도 broadcast 메시지는 한 묶음으로 유지되고 묶음 대표 순서가 보존된다")
    void getMessages_GroupsBroadcastMessages_InAscendingOrder() {
        Pageable pageable = PageRequest.of(0, 20);
        Pageable fetchAllAscending = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Order.asc("id")));
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 1L, "B팀");
        TeamThread firstThread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamThread secondThread = TeamThread.builder().id(200L).teamId(20L).build();
        TeamMessage first = message(1001L, 100L, "전체 공지", "broadcast-1");
        TeamMessage second = message(1002L, 200L, "전체 공지", "broadcast-1");
        TeamMessage standalone = message(1003L, 100L, "개별 공지");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(firstTeam, secondTeam));
        given(teamThreadQueryService.getThreads(List.of(10L, 20L))).willReturn(List.of(firstThread, secondThread));
        given(teamMessageQueryService.getMessages(List.of(100L, 200L), null, fetchAllAscending))
            .willReturn(new PageImpl<>(List.of(first, second, standalone), fetchAllAscending, 3));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1001L, 1003L)))
            .willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L, 200L), PROFESSOR_ID)).willReturn(2L);
        given(teamMessageQueryService.getMessagesByBroadcastId("broadcast-1"))
            .willReturn(List.of(first, second));

        var response = teamMessageAdminFacade.getMessages(
            1L, null, null, null, Sort.Direction.ASC, pageable, PROFESSOR_ID);

        assertThat(response.contents()).extracting(content -> content.id())
            .containsExactly(1001L, 1003L);
        assertThat(response.contents().get(0).targetTeamCount()).isEqualTo(2);
        assertThat(response.pageable().totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("teamId로 필터링해도 대상 팀 수는 필터와 무관하게 실제 발송 대상 팀 수를 그대로 보여준다")
    void getMessages_TargetTeamCount_IgnoresTeamIdFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 1L, "B팀");
        TeamThread firstThread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamMessage first = message(1001L, 100L, "전체 공지", "broadcast-1");
        TeamMessage second = message(1002L, 200L, "전체 공지", "broadcast-1");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(firstTeam, secondTeam));
        given(teamThreadQueryService.getThreads(List.of(10L))).willReturn(List.of(firstThread));
        given(teamMessageQueryService.getMessages(List.of(100L), null, FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(first), FETCH_ALL_PAGEABLE, 1));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1001L))).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L), PROFESSOR_ID)).willReturn(1L);
        // broadcast-1은 원래 26팀 중 2팀(A팀, B팀)에 보내졌지만, 지금 조회는 teamId=10(A팀)으로 좁혀져 있다.
        given(teamMessageQueryService.getMessagesByBroadcastId("broadcast-1"))
            .willReturn(List.of(first, second));

        var response = teamMessageAdminFacade.getMessages(
            1L, 10L, null, null, Sort.Direction.DESC, pageable, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content ->
            assertThat(content.targetTeamCount()).isEqualTo(2));
    }

    @Test
    @DisplayName("page*size가 int 범위를 넘는 큰 페이지 번호를 요청해도 오버플로 없이 빈 페이지를 반환한다")
    void getMessages_LargePageNumber_DoesNotOverflow() {
        // page=1_000_000_000, size=10처럼 곱이 int 범위(약 21억)를 넘는 경우를 재현한다.
        Pageable hugePage = PageRequest.of(1_000_000_000, 10);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team team = team(10L, 1L, "A팀");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamMessage message = message(1000L, 100L, "확인 부탁드립니다.");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(team));
        given(teamThreadQueryService.getThreads(List.of(10L))).willReturn(List.of(thread));
        given(teamMessageQueryService.getMessages(List.of(100L), null, FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(message), FETCH_ALL_PAGEABLE, 1));
        given(teamMessageQueryService.countUnread(List.of(100L), PROFESSOR_ID)).willReturn(0L);

        var response = teamMessageAdminFacade.getMessages(1L, hugePage, PROFESSOR_ID);

        assertThat(response.contents()).isEmpty();
        assertThat(response.pageable().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("지정한 teamId가 교수의 담당 분반에 속하지 않으면 거부한다")
    void getMessages_ForeignTeamId() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team firstTeam = team(10L, 1L, "A팀");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L)))
            .willReturn(List.of(firstTeam));

        assertThatThrownBy(() -> teamMessageAdminFacade.getMessages(
            1L, 999L, TeamMessageRelatedType.MID_REPORT, null, Sort.Direction.DESC, pageable, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 팀 메시지만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("senderKeyword를 지정하면 조회 범위 내 이름 또는 학번이 일치하는 발신자의 메시지만 조회한다")
    void getMessages_WithSenderKeyword() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team team = team(10L, 1L, "A팀");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();
        TeamMessage message = message(1000L, 100L, "화면설계서 확인 부탁드립니다.");

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(team));
        given(teamThreadQueryService.getThreads(List.of(10L))).willReturn(List.of(thread));
        given(teamMessageQueryService.findDistinctSenderIds(List.of(100L)))
            .willReturn(List.of("202612345"));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of("202612345")))
            .willReturn(List.of(User.create("202612345", "kgu@kyonggi.ac.kr", "김학생", "encoded",
                UserGlobalRole.USER, "010-1234-5678")));
        given(teamMessageQueryService.getMessages(List.of(100L), null, List.of("202612345"), FETCH_ALL_PAGEABLE))
            .willReturn(new PageImpl<>(List.of(message), FETCH_ALL_PAGEABLE, 1));
        given(teamMessageQueryService.findReadMessageIds(PROFESSOR_ID, List.of(1000L))).willReturn(Set.of());
        given(teamMessageQueryService.countUnread(List.of(100L), PROFESSOR_ID)).willReturn(1L);

        var response = teamMessageAdminFacade.getMessages(
            1L, null, null, "김학생", Sort.Direction.DESC, pageable, PROFESSOR_ID);

        assertThat(response.contents()).singleElement().satisfies(content ->
            assertThat(content.message()).isEqualTo("화면설계서 확인 부탁드립니다."));
        verify(teamMessageQueryService).getMessages(List.of(100L), null, List.of("202612345"), FETCH_ALL_PAGEABLE);
    }

    @Test
    @DisplayName("senderKeyword에 일치하는 발신자가 없으면 메시지 조회 없이 빈 목록을 반환하되, 안 읽은 개수는 검색과 무관하게 분반 전체 기준으로 응답한다")
    void getMessages_WithSenderKeyword_NoMatch() {
        Pageable pageable = PageRequest.of(0, 20);
        Section section = section(1L, "월3,4", "1151", PROFESSOR_ID);
        Team team = team(10L, 1L, "A팀");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();

        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));
        given(teamRepository.findAllBySectionIdIn(List.of(1L))).willReturn(List.of(team));
        given(teamThreadQueryService.getThreads(List.of(10L))).willReturn(List.of(thread));
        given(teamMessageQueryService.findDistinctSenderIds(List.of(100L)))
            .willReturn(List.of("202612345"));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(anyList()))
            .willReturn(List.of(User.create("202612345", "kgu@kyonggi.ac.kr", "김학생", "encoded",
                UserGlobalRole.USER, "010-1234-5678")));
        given(teamMessageQueryService.countUnread(List.of(100L), PROFESSOR_ID)).willReturn(3L);

        var response = teamMessageAdminFacade.getMessages(
            1L, null, null, "존재하지않는이름", Sort.Direction.DESC, pageable, PROFESSOR_ID);

        assertThat(response.contents()).isEmpty();
        assertThat(response.unreadCount()).isEqualTo(3L);
        verify(teamMessageQueryService, never()).getMessages(anyList(), any(), anyList(), any());
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
        given(teamMessageQueryService.resolveMarkAsReadTargetIds(message, true)).willReturn(List.of(1000L));

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
    @DisplayName("일괄 발송으로 묶인 메시지는 하나를 읽음 처리하면 같은 broadcastId의 나머지도 전부 읽음 처리된다")
    void markAsRead_BroadcastGroup_MarksAllSiblingsAsRead() {
        TeamMessage message = message(1001L, 100L, "전체 공지", "broadcast-1");
        TeamThread thread = TeamThread.builder().id(100L).teamId(10L).build();
        Team team = team(10L, 1L, "A팀");
        given(teamMessageQueryService.getMessage(1001L)).willReturn(message);
        given(teamThreadQueryService.getThreadById(100L)).willReturn(thread);
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(teamMessageQueryService.resolveMarkAsReadTargetIds(message, true))
            .willReturn(List.of(1001L, 1002L));

        teamMessageAdminFacade.markAsRead(1001L, PROFESSOR_ID);

        verify(teamMessageCommandService).markAsRead(1001L, PROFESSOR_ID);
        verify(teamMessageCommandService).markAsRead(1002L, PROFESSOR_ID);
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

    @Test
    @DisplayName("수신 팀 목록은 메시지 순서와 무관하게 팀 조회 결과(팀 ID순) 순서로 반환한다")
    void getRecipientTeams_ReturnsTeamsInRepositoryOrder() {
        TeamMessage representative = message(2001L, 200L, "전체 공지", "broadcast-1");
        TeamMessage sibling = message(2002L, 100L, "전체 공지", "broadcast-1");
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 1L, "B팀");

        given(teamMessageQueryService.getMessage(2001L)).willReturn(representative);
        given(teamThreadQueryService.getThreadById(200L))
            .willReturn(TeamThread.builder().id(200L).teamId(20L).build());
        given(teamThreadQueryService.getThreadById(100L))
            .willReturn(TeamThread.builder().id(100L).teamId(10L).build());
        given(teamRepository.findById(20L)).willReturn(Optional.of(secondTeam));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(teamMessageQueryService.getMessagesByBroadcastId("broadcast-1"))
            .willReturn(List.of(representative, sibling));
        given(teamRepository.findAllById(List.of(20L, 10L))).willReturn(List.of(firstTeam, secondTeam));
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(20L, 10L)))
            .willReturn(java.util.Map.of());

        var recipients = teamMessageAdminFacade.getRecipientTeams(2001L, PROFESSOR_ID);

        assertThat(recipients).extracting(r -> r.teamId()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("일괄발송으로 묶인 메시지는 대표 메시지의 분반 소유권만 검증하고 수신 팀 전체를 반환한다")
    void getRecipientTeams_OwnedBroadcastGroup_ReturnsAllRecipients() {
        TeamMessage representative = message(2001L, 100L, "전체 공지", "broadcast-1");
        TeamMessage sibling = message(2002L, 200L, "전체 공지", "broadcast-1");
        Team firstTeam = team(10L, 1L, "A팀");
        Team secondTeam = team(20L, 1L, "B팀");

        given(teamMessageQueryService.getMessage(2001L)).willReturn(representative);
        given(teamThreadQueryService.getThreadById(100L))
            .willReturn(TeamThread.builder().id(100L).teamId(10L).build());
        given(teamRepository.findById(10L)).willReturn(Optional.of(firstTeam));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(teamMessageQueryService.getMessagesByBroadcastId("broadcast-1"))
            .willReturn(List.of(representative, sibling));
        given(teamThreadQueryService.getThreadById(200L))
            .willReturn(TeamThread.builder().id(200L).teamId(20L).build());
        given(teamRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(firstTeam, secondTeam));
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L, 20L)))
            .willReturn(java.util.Map.of(10L, "김리더"));

        var recipients = teamMessageAdminFacade.getRecipientTeams(2001L, PROFESSOR_ID);

        assertThat(recipients).hasSize(2);
        assertThat(recipients.get(0).teamId()).isEqualTo(10L);
        assertThat(recipients.get(0).teamName()).isEqualTo("A팀");
        assertThat(recipients.get(0).leaderName()).isEqualTo("김리더");
        assertThat(recipients.get(1).teamId()).isEqualTo(20L);
        assertThat(recipients.get(1).teamName()).isEqualTo("B팀");
        assertThat(recipients.get(1).leaderName()).isNull();
    }

    @Test
    @DisplayName("다른 교수 분반의 일괄발송 메시지는 수신 팀 목록을 조회할 수 없다")
    void getRecipientTeams_ForeignSection_ThrowsAccessDenied() {
        TeamMessage representative = message(2001L, 100L, "전체 공지", "broadcast-1");
        Team team = team(10L, 1L, "A팀");

        given(teamMessageQueryService.getMessage(2001L)).willReturn(representative);
        given(teamThreadQueryService.getThreadById(100L))
            .willReturn(TeamThread.builder().id(100L).teamId(10L).build());
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> teamMessageAdminFacade.getRecipientTeams(2001L, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("담당 분반의 메시지만 조회할 수 있습니다.");

        verify(teamMessageQueryService, never()).getMessagesByBroadcastId(any());
    }

    @Test
    @DisplayName("일괄발송이 아닌 단일 전송 메시지는 그 팀 하나만 반환한다")
    void getRecipientTeams_SingleMessage_ReturnsThatTeamOnly() {
        TeamMessage single = message(3001L, 100L, "확인했습니다.");
        Team team = team(10L, 1L, "A팀");

        given(teamMessageQueryService.getMessage(3001L)).willReturn(single);
        given(teamThreadQueryService.getThreadById(100L))
            .willReturn(TeamThread.builder().id(100L).teamId(10L).build());
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findAllById(List.of(10L))).willReturn(List.of(team));
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L))).willReturn(java.util.Map.of());

        var recipients = teamMessageAdminFacade.getRecipientTeams(3001L, PROFESSOR_ID);

        assertThat(recipients).singleElement().satisfies(recipient -> {
            assertThat(recipient.teamId()).isEqualTo(10L);
            assertThat(recipient.teamName()).isEqualTo("A팀");
            assertThat(recipient.leaderName()).isNull();
        });
        verify(teamMessageQueryService, never()).getMessagesByBroadcastId(any());
    }

    @Test
    @DisplayName("일괄발송으로 생성됐지만 대상 팀이 1개뿐이어도 그 팀을 반환한다")
    void getRecipientTeams_BroadcastGroupOfOne_ReturnsThatTeamOnly() {
        TeamMessage representative = message(2001L, 100L, "전체 공지", "broadcast-2");
        Team team = team(10L, 1L, "A팀");

        given(teamMessageQueryService.getMessage(2001L)).willReturn(representative);
        given(teamThreadQueryService.getThreadById(100L))
            .willReturn(TeamThread.builder().id(100L).teamId(10L).build());
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, PROFESSOR_ID)).willReturn(true);
        given(teamMessageQueryService.getMessagesByBroadcastId("broadcast-2"))
            .willReturn(List.of(representative));
        given(teamRepository.findAllById(List.of(10L))).willReturn(List.of(team));
        given(teamMemberQueryService.getLeaderNamesByTeamIds(List.of(10L))).willReturn(java.util.Map.of());

        var recipients = teamMessageAdminFacade.getRecipientTeams(2001L, PROFESSOR_ID);

        assertThat(recipients).singleElement().satisfies(recipient -> {
            assertThat(recipient.teamId()).isEqualTo(10L);
            assertThat(recipient.teamName()).isEqualTo("A팀");
        });
    }

    private Section section(Long id, String classTime, String code, String professorId) {
        return Section.builder()
            .id(id)
            .professorId(professorId)
            .classTime(classTime)
            .code(code)
            .build();
    }

    private SectionDetail detail(Section section) {
        return new SectionDetail(section, null, null);
    }

    private Team team(Long id, Long sectionId, String name) {
        return Team.builder().id(id).sectionId(sectionId).name(name).build();
    }

    private TeamMessage message(Long id, Long threadId, String content) {
        return message(id, threadId, content, null);
    }

    private TeamMessage message(Long id, Long threadId, String content, String broadcastId) {
        return TeamMessage.builder()
            .id(id)
            .threadId(threadId)
            .senderId("202612345")
            .message(content)
            .relatedType(TeamMessageRelatedType.GENERAL)
            .broadcastId(broadcastId)
            .createdAt(LocalDateTime.of(2026, 8, 25, 19, 30))
            .build();
    }
}
