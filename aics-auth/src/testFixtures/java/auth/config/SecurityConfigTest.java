package auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kgu.developers.auth.api.application.AuthFacade;
import kgu.developers.auth.api.presentation.AuthControllerImpl;
import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.auth.config.SecurityConfig;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@WebMvcTest
@Import({SecurityConfig.class, JwtCookieAuthenticationFilter.class, JwtUtil.class, CorsConfig.class,
    AuthControllerImpl.class})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173"
})
class SecurityConfigTest {

  private static final String LOGIN_URL = "/api/v1/oop/auth/login";
  private static final String REFRESH_URL = "/api/v1/oop/auth/refresh";
  private static final String LOGOUT_URL = "/api/v1/oop/auth/logout";
  private static final String CSRF_COOKIE = "XSRF-TOKEN";
  private static final String LOGIN_BODY = """
      {"studentNumber":"202699999","password":"12345678"}""";

  @SpringBootConfiguration
  static class TestApp {
  }

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthFacade authFacade;

  @MockitoBean
  private TokenRevocationStore tokenRevocationStore;

  @Test
  @DisplayName("login은 CSRF 토큰 없이 통과한다")
  void loginWithoutCsrfToken() throws Exception {
    given(authFacade.login(any())).willReturn(LoginResponse.of("access-token", "refresh-token"));

    mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("오류 페이지 디스패치는 인증을 요구하지 않는다")
  void errorDispatchIsPermitted() throws Exception {
    mockMvc.perform(get("/error").with(request -> {
          request.setDispatcherType(DispatcherType.ERROR);
          return request;
        }))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
  }

  @Test
  @DisplayName("CSRF 토큰이 없는 refresh는 403을 응답한다 (강제 토큰 회전 차단)")
  void refreshWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(REFRESH_URL))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("XSRF-TOKEN 쿠키는 세션 쿠키가 아니라 refresh 유효기간보다 오래 산다")
  void csrfCookieOutlivesBrowserSession() throws Exception {
    given(authFacade.login(any())).willReturn(LoginResponse.of("access-token", "refresh-token"));

    Cookie csrfCookie = mockMvc
        .perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
        .andReturn().getResponse().getCookie(CSRF_COOKIE);

    // 브라우저를 껐다 켜도 남아야 refreshToken(P14D)만 남고 CSRF 토큰이 없어 /refresh가 403 나는 일이 없다
    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getMaxAge()).isGreaterThan((int) Duration.ofDays(14).toSeconds());
  }

  @Test
  @DisplayName("이미 XSRF-TOKEN을 가진 요청도 같은 토큰을 다시 내려받아 수명이 갱신된다")
  void csrfCookieSlidesOnEveryResponse() throws Exception {
    Cookie issued = mockMvc.perform(get("/api/v1/oop/auth/none"))
        .andReturn().getResponse().getCookie(CSRF_COOKIE);

    // 쿠키가 있을 때 재발급을 멈추면 만료 시각이 고정돼, 결국 refreshToken보다 먼저 죽는다
    Cookie renewed = mockMvc.perform(get("/api/v1/oop/auth/none").cookie(issued))
        .andReturn().getResponse().getCookie(CSRF_COOKIE);

    assertThat(renewed).isNotNull();
    assertThat(renewed.getValue()).isEqualTo(issued.getValue());
    assertThat(renewed.getMaxAge()).isEqualTo(issued.getMaxAge());
  }

  @Test
  @DisplayName("CSRF 토큰이 없는 logout은 403을 응답한다 (강제 로그아웃 차단)")
  void logoutWithoutCsrfToken() throws Exception {
    mockMvc.perform(post(LOGOUT_URL))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CSRF 토큰을 담은 logout은 통과한다")
  void logoutWithCsrfToken() throws Exception {
    mockMvc.perform(post(LOGOUT_URL).with(csrf()))
        .andExpect(status().isOk());
  }

}
