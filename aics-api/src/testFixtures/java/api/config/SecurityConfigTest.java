package api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import kgu.developers.api.config.SecurityConfig;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

/**
 * 컨트롤러가 아니라 필터 체인 자체를 검증한다. 그래서 존재하지 않는 경로로 찔러본다 —
 * 인증·CSRF는 핸들러보다 앞에서 끝나므로, 통과하면 404가 나오고 막히면 401/403이 나온다.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtCookieAuthenticationFilter.class, JwtUtil.class, CorsConfig.class})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173"
})
class SecurityConfigTest {

  private static final String PROTECTED_URL = "/api/v1/oop/anything";
  private static final String STUDENT_NUMBER = "202699999";
  private static final String ORIGIN = "http://localhost:5173";

  @SpringBootConfiguration
  static class TestApp {
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtUtil jwtUtil;

  // Redis 없이 도는 슬라이스 테스트라 무효화 조회는 대역으로 둔다 (기본값 false = 무효화 안 됨).
  @MockitoBean
  private TokenRevocationStore tokenRevocationStore;

  private Cookie accessTokenCookie() {
    return new Cookie("accessToken", jwtUtil.createAccessToken(STUDENT_NUMBER, "USER"));
  }

  @Test
  @DisplayName("쿠키가 없으면 401을 응답한다")
  void unauthenticated() throws Exception {
    mockMvc.perform(get(PROTECTED_URL))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("정상 accessToken 쿠키는 인증을 통과한다")
  void authenticated() throws Exception {
    mockMvc.perform(get(PROTECTED_URL).cookie(accessTokenCookie()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("위조된 accessToken 쿠키는 401을 응답한다")
  void forgedAccessTokenCookie() throws Exception {
    mockMvc.perform(get(PROTECTED_URL).cookie(new Cookie("accessToken", "not-a-jwt")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("빈 accessToken 쿠키도 500이 아니라 401을 응답한다")
  void blankAccessTokenCookie() throws Exception {
    mockMvc.perform(get(PROTECTED_URL).cookie(new Cookie("accessToken", "")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("무효화된 accessToken 쿠키는 서명이 멀쩡해도 401을 응답한다")
  void revokedAccessTokenCookie() throws Exception {
    given(tokenRevocationStore.isRevoked(eq(STUDENT_NUMBER), any())).willReturn(true);

    mockMvc.perform(get(PROTECTED_URL).cookie(accessTokenCookie()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("CSRF 토큰이 없는 쓰기 요청은 403을 응답한다")
  void writeWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(PROTECTED_URL).cookie(accessTokenCookie()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CSRF 토큰을 담은 쓰기 요청은 통과한다")
  void writeWithCsrfToken() throws Exception {
    mockMvc.perform(post(PROTECTED_URL).cookie(accessTokenCookie()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("API 문서 경로는 인증 없이 열려 있다")
  void docsArePermitted() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
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
    mockMvc.perform(options(PROTECTED_URL)
            .header(HttpHeaders.ORIGIN, ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  @DisplayName("허용되지 않은 origin의 preflight 요청은 403을 응답한다")
  void preflightFromForeignOrigin() throws Exception {
    mockMvc.perform(options(PROTECTED_URL)
            .header(HttpHeaders.ORIGIN, "http://evil.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }
}
