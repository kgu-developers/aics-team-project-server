package config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.user.application.UserAdminFacade;
import kgu.developers.admin.user.presentation.UserAdminControllerImpl;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@WebMvcTest
@Import({SecurityConfig.class, JwtCookieAuthenticationFilter.class, JwtUtil.class, CorsConfig.class,
    UserAdminControllerImpl.class})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173"
})
class SecurityConfigTest {

  private static final String ADMIN_URL = "/api/v1/admin/oop/users";
  private static final String STUDENT_NUMBER = "202699999";
  private static final String ORIGIN = "http://localhost:5173";

  @SpringBootConfiguration
  static class TestApp {
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtUtil jwtUtil;

  @MockitoBean
  private UserAdminFacade userAdminFacade;

  // Redis 없이 도는 슬라이스 테스트라 무효화 조회는 대역으로 둔다 (기본값 false = 무효화 안 됨).
  @MockitoBean
  private TokenRevocationStore tokenRevocationStore;

  private Cookie accessTokenCookie(String role) {
    return new Cookie("accessToken", jwtUtil.createAccessToken(STUDENT_NUMBER, role));
  }

  @Test
  @DisplayName("미인증 요청은 401을 응답한다")
  void unauthenticated() throws Exception {
    mockMvc.perform(get(ADMIN_URL))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("일반 사용자 요청은 403을 응답한다")
  @WithMockUser(roles = "USER")
  void notAdmin() throws Exception {
    mockMvc.perform(get(ADMIN_URL))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("관리자 요청은 200을 응답한다")
  @WithMockUser(roles = "ADMIN")
  void admin() throws Exception {
    given(userAdminFacade.getAllUsers()).willReturn(UserAdminListResponse.from(List.of()));

    mockMvc.perform(get(ADMIN_URL))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("ADMIN 역할의 accessToken 쿠키를 담으면 200을 응답한다")
  void adminAccessTokenCookie() throws Exception {
    given(userAdminFacade.getAllUsers()).willReturn(UserAdminListResponse.from(List.of()));

    mockMvc.perform(get(ADMIN_URL).cookie(accessTokenCookie("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("USER 역할의 accessToken 쿠키는 403을 응답한다")
  void studentAccessTokenCookie() throws Exception {
    mockMvc.perform(get(ADMIN_URL).cookie(accessTokenCookie("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ASSISTANT 역할의 accessToken 쿠키는 403을 응답한다")
  void assistantAccessTokenCookie() throws Exception {
    mockMvc.perform(get(ADMIN_URL).cookie(accessTokenCookie("ASSISTANT")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("무효화된 accessToken 쿠키는 서명이 멀쩡해도 401을 응답한다 (탈퇴·강등 즉시 반영)")
  void revokedAccessTokenCookie() throws Exception {
    given(tokenRevocationStore.isRevoked(org.mockito.ArgumentMatchers.eq(STUDENT_NUMBER),
        org.mockito.ArgumentMatchers.any())).willReturn(true);

    mockMvc.perform(get(ADMIN_URL).cookie(accessTokenCookie("ADMIN")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("위조된 accessToken 쿠키는 401을 응답한다")
  void forgedAccessTokenCookie() throws Exception {
    mockMvc.perform(get(ADMIN_URL).cookie(new Cookie("accessToken", "not-a-jwt")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("HTTP Basic 자격 증명으로는 통과하지 못한다 (인증은 JWT 쿠키 전용)")
  void httpBasicIsRejected() throws Exception {
    String credentials = Base64.getEncoder().encodeToString("admin:admin".getBytes(UTF_8));

    mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.AUTHORIZATION, "Basic " + credentials))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("API 문서 경로는 인증 없이 열려 있다")
  void docsArePermitted() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
  }

  @Test
  @DisplayName("CSRF 토큰이 없는 관리자 요청은 403을 응답한다")
  @WithMockUser(roles = "ADMIN")
  void adminWriteWithoutCsrfToken() throws Exception {
    mockMvc.perform(delete(ADMIN_URL + "/" + STUDENT_NUMBER))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CSRF 토큰을 담은 관리자 요청은 통과한다")
  @WithMockUser(roles = "ADMIN")
  void adminWriteWithCsrfToken() throws Exception {
    mockMvc.perform(delete(ADMIN_URL + "/" + STUDENT_NUMBER).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("오류 페이지 디스패치는 인증을 요구하지 않는다")
  void errorDispatchIsPermitted() throws Exception {
    // 컨테이너가 오류를 렌더링하려고 다시 디스패치할 때 이 체인이 막으면,
    // 실제 400·405·500이 전부 본문 없는 401로 바뀐다.
    mockMvc.perform(get("/error").with(request -> {
          request.setDispatcherType(DispatcherType.ERROR);
          return request;
        }))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
  }

  @Test
  @DisplayName("허용된 origin의 preflight 요청은 인증 없이 통과한다")
  void preflight() throws Exception {
    mockMvc.perform(options(ADMIN_URL)
            .header(HttpHeaders.ORIGIN, ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  @DisplayName("허용되지 않은 origin의 preflight 요청은 403을 응답한다")
  void preflightFromForeignOrigin() throws Exception {
    mockMvc.perform(options(ADMIN_URL)
            .header(HttpHeaders.ORIGIN, "http://evil.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }
}
