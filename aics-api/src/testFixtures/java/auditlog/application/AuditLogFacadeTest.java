package auditlog.application;

import static kgu.developers.domain.auditLog.domain.TargetType.TEAM;
import static kgu.developers.domain.user.domain.UserGlobalRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.auditlog.application.AuditLogFacade;
import kgu.developers.api.auditlog.presentation.response.TeamActivitySummaryResponse;
import kgu.developers.api.auditlog.presentation.response.TeamHistoryPageResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.application.query.AuditLogQueryService;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class AuditLogFacadeTest {

    private static final Long TEAM_ID = 3L;
    private static final Long SECTION_ID = 2L;
    private static final String USER_A = "202600001";
    private static final String USER_B = "202600002";

    @Mock
    private TeamAccessValidator teamAccessValidator;
    @Mock
    private TeamMemberQueryService teamMemberQueryService;
    @Mock
    private AuditLogQueryService auditLogQueryService;
    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private AuditLogFacade auditLogFacade;

    @Test
    @DisplayName("getTeamHistories는 해당 팀을 대상으로 한 변경 이력을 행위자 이름과 함께 반환한다")
    void getTeamHistories() {
        PageRequest pageable = PageRequest.of(0, 10);
        AuditLog history = auditLog(1L, USER_A, "TEAM_NAME_UPDATED", TEAM, TEAM_ID,
                LocalDateTime.of(2026, 9, 1, 10, 0));
        given(teamAccessValidator.validateMembershipOrProfessor(TEAM_ID, USER_B)).willReturn(team());
        given(auditLogQueryService.getTeamHistories(SECTION_ID, TEAM_ID, pageable))
                .willReturn(new PageImpl<>(List.of(history), pageable, 1));
        given(userQueryService.getUsersByStudentNumbers(List.of(USER_A)))
                .willReturn(List.of(user(USER_A, "김태양", null)));

        TeamHistoryPageResponse result = auditLogFacade.getTeamHistories(TEAM_ID, pageable, USER_B);

        verify(teamAccessValidator).validateMembershipOrProfessor(TEAM_ID, USER_B);
        assertThat(result.contents()).hasSize(1);
        assertThat(result.contents().get(0).actorName()).isEqualTo("김태양");
        assertThat(result.contents().get(0).eventType()).isEqualTo("TEAM_NAME_UPDATED");
        assertThat(result.pageable().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getTeamActivitySummary는 팀원별 마지막 로그인과 가장 최근 활동만 반환한다")
    void getTeamActivitySummary() {
        LocalDateTime loginAt = LocalDateTime.of(2026, 9, 1, 9, 0);
        List<TeamMember> members = List.of(
                TeamMember.create(TEAM_ID, USER_B, true, "발표"),
                TeamMember.create(TEAM_ID, USER_A, false, "기록")
        );
        AuditLog older = auditLog(1L, USER_B, "TEAM_RULE_UPDATED", TEAM, TEAM_ID,
                LocalDateTime.of(2026, 9, 1, 9, 30));
        AuditLog latest = auditLog(2L, USER_B, "TEAM_NAME_UPDATED", TEAM, TEAM_ID,
                LocalDateTime.of(2026, 9, 1, 10, 30));
        AuditLog outsider = auditLog(3L, "202699999", "TEAM_UPDATED", TEAM, TEAM_ID,
                LocalDateTime.of(2026, 9, 1, 11, 0));
        given(teamAccessValidator.validateMembershipOrProfessor(TEAM_ID, USER_A)).willReturn(team());
        given(teamMemberQueryService.getTeamMembersByTeamId(TEAM_ID)).willReturn(members);
        given(userQueryService.getUsersByStudentNumbers(List.of(USER_B, USER_A)))
                .willReturn(List.of(user(USER_A, "김태양", null), user(USER_B, "이학생", loginAt)));
        given(auditLogQueryService.getMemberActivities(SECTION_ID, TEAM_ID, List.of(USER_B, USER_A)))
                .willReturn(List.of(older, outsider, latest));

        TeamActivitySummaryResponse result = auditLogFacade.getTeamActivitySummary(TEAM_ID, USER_A);

        assertThat(result.members()).extracting("userId").containsExactly(USER_B, USER_A);
        assertThat(result.members().get(0).lastLoginAt()).isEqualTo(loginAt);
        assertThat(result.members().get(0).lastActivity().eventType()).isEqualTo("TEAM_NAME_UPDATED");
        assertThat(result.members().get(1).lastLoginAt()).isNull();
        assertThat(result.members().get(1).lastActivity()).isNull();
        verify(auditLogQueryService).getMemberActivities(SECTION_ID, TEAM_ID, List.of(USER_B, USER_A));
    }

    @Test
    @DisplayName("팀원 또는 담당 교수가 아니면 감사 로그를 조회할 수 없다")
    void deniesOutsider() {
        given(teamAccessValidator.validateMembershipOrProfessor(TEAM_ID, "outsider"))
                .willThrow(new AccessDeniedException("접근할 수 없습니다."));

        assertThatThrownBy(() -> auditLogFacade.getTeamHistories(
                TEAM_ID, PageRequest.of(0, 10), "outsider"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> auditLogFacade.getTeamActivitySummary(TEAM_ID, "outsider"))
                .isInstanceOf(AccessDeniedException.class);
        verify(auditLogQueryService, never()).getTeamHistories(any(), any(), any());
    }

    private Team team() {
        return Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("3팀").build();
    }

    private User user(String studentNumber, String name, LocalDateTime lastLoginAt) {
        return User.builder()
                .studentNumber(studentNumber)
                .email(studentNumber + "@kyonggi.ac.kr")
                .name(name)
                .password("hashed")
                .globalRole(USER)
                .phone("010-0000-0000")
                .lastLoginAt(lastLoginAt)
                .build();
    }

    private AuditLog auditLog(
            Long id,
            String actorId,
            String eventType,
            kgu.developers.domain.auditLog.domain.TargetType targetType,
            Long targetId,
            LocalDateTime createdAt
    ) {
        return AuditLog.builder()
                .id(id)
                .actorId(actorId)
                .sectionId(SECTION_ID)
                .eventType(eventType)
                .targetType(targetType)
                .targetId(targetId)
                .metadata(JsonConverter.parse("{}"))
                .createdAt(createdAt)
                .build();
    }
}
