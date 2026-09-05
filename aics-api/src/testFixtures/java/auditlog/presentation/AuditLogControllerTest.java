package auditlog.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import kgu.developers.api.auditlog.application.AuditLogFacade;
import kgu.developers.api.auditlog.presentation.AuditLogControllerImpl;
import kgu.developers.api.auditlog.presentation.response.TeamActivitySummaryResponse;
import kgu.developers.api.auditlog.presentation.response.TeamHistoryPageResponse;
import kgu.developers.api.config.SecurityConfig;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@WebMvcTest
@Import({SecurityConfig.class, JwtCookieAuthenticationFilter.class, JwtUtil.class,
        AuditLogControllerImpl.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
        "jwt.issuer=kgudevelopers@gmail.com"
})
class AuditLogControllerTest {

    private static final String USER_ID = "202699999";

    @SpringBootConfiguration
    static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuditLogFacade auditLogFacade;

    @MockitoBean
    private TokenRevocationStore tokenRevocationStore;

    @Test
    @DisplayName("인증된 사용자는 팀 변경 이력을 조회할 수 있다")
    void getTeamHistories() throws Exception {
        TeamHistoryPageResponse response = new TeamHistoryPageResponse(
                List.of(),
                new PageableResponse<>(0, 20, 0, 0, true)
        );
        org.mockito.BDDMockito.given(auditLogFacade.getTeamHistories(eq(3L), any(Pageable.class), eq(USER_ID)))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/teams/{teamId}/histories", 3L)
                        .cookie(accessTokenCookie()))
                .andExpect(status().isOk());

        verify(auditLogFacade).getTeamHistories(eq(3L), any(Pageable.class), eq(USER_ID));
    }

    @Test
    @DisplayName("인증된 사용자는 팀원 활동 요약을 조회할 수 있다")
    void getTeamActivitySummary() throws Exception {
        org.mockito.BDDMockito.given(auditLogFacade.getTeamActivitySummary(3L, USER_ID))
                .willReturn(new TeamActivitySummaryResponse(3L, List.of()));

        mockMvc.perform(get("/api/v1/teams/{teamId}/activity-summary", 3L)
                        .cookie(accessTokenCookie()))
                .andExpect(status().isOk());

        verify(auditLogFacade).getTeamActivitySummary(3L, USER_ID);
    }

    @Test
    @DisplayName("팀 식별자가 양수가 아니면 400을 응답한다")
    void invalidTeamId() throws Exception {
        mockMvc.perform(get("/api/v1/teams/{teamId}/histories", 0L)
                        .cookie(accessTokenCookie()))
                .andExpect(status().isBadRequest());

        verify(auditLogFacade, never()).getTeamHistories(any(), any(), any());
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 응답한다")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/teams/{teamId}/activity-summary", 3L))
                .andExpect(status().isUnauthorized());

        verify(auditLogFacade, never()).getTeamActivitySummary(any(), any());
    }

    private Cookie accessTokenCookie() {
        return new Cookie("accessToken", jwtUtil.createAccessToken(USER_ID, "USER"));
    }
}
