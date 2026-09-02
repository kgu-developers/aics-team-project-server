package auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import kgu.developers.auth.api.application.AuthFacade;
import kgu.developers.auth.api.presentation.AuthControllerImpl;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.domain.auth.domain.LoginRole;
import kgu.developers.globalutils.jwt.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private static final String URL = "/api/v1/oop/auth/login";
  private static final String BODY = """
      {"studentNumber":"202699999","password":"12345678"}""";

  @Mock
  private AuthFacade authFacade;

  @Mock
  private JwtUtil jwtUtil;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AuthControllerImpl(authFacade, jwtUtil, false)).build();
  }

  private MvcResult login() throws Exception {
    given(authFacade.login(new LoginRequest("202699999", "12345678")))
        .willReturn(LoginResponse.of("access-token", "refresh-token", LoginRole.STUDENT));
    given(jwtUtil.getAccessTokenValidity()).willReturn(Duration.ofMinutes(30));
    given(jwtUtil.getRefreshTokenValidity()).willReturn(Duration.ofDays(14));

    return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  @DisplayName("로그인 성공 시 본문은 message와 role이고 토큰은 Set-Cookie로 내려간다")
  void loginSetsCookiesNotBody() throws Exception {
    MvcResult result = login();

    assertThat(result.getResponse().getContentAsString())
        .isEqualTo("{\"message\":\"Login Successfully\",\"role\":\"STUDENT\"}");
    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .anyMatch(c -> c.startsWith("accessToken=access-token"))
        .anyMatch(c -> c.startsWith("refreshToken=refresh-token"));
  }

  @Test
  @DisplayName("토큰 쿠키는 HttpOnly이고 만료가 토큰 유효기간과 같다")
  void tokenCookiesAreHttpOnly() throws Exception {
    MvcResult result = login();

    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .allMatch(c -> c.contains("HttpOnly"))
        .allMatch(c -> c.contains("SameSite=Lax"))
        .allMatch(c -> c.contains("Path=/"));

    assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=1800");
  }

  @Test
  @DisplayName("refresh는 refreshToken 쿠키를 읽어 새 토큰 쿠키를 내려준다")
  void refreshReadsRefreshTokenCookie() throws Exception {
    given(authFacade.refresh("old-refresh-token"))
        .willReturn(LoginResponse.of("new-access-token", "new-refresh-token", LoginRole.STUDENT));
    given(jwtUtil.getAccessTokenValidity()).willReturn(Duration.ofMinutes(30));
    given(jwtUtil.getRefreshTokenValidity()).willReturn(Duration.ofDays(14));

    MvcResult result = mockMvc.perform(post("/api/v1/oop/auth/refresh")
            .cookie(new Cookie("refreshToken", "old-refresh-token")))
        .andExpect(status().isOk())
        .andReturn();

    assertThat(result.getResponse().getContentAsString()).isEqualTo("{\"message\":\"Refresh Successfully\",\"role\":\"STUDENT\"}");
    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .anyMatch(c -> c.startsWith("accessToken=new-access-token"))
        .anyMatch(c -> c.startsWith("refreshToken=new-refresh-token"))
        .allMatch(c -> c.contains("HttpOnly"));
  }

  @Test
  @DisplayName("refresh는 쿠키가 없어도 파사드까지 도달한다 (null 처리는 파사드 책임)")
  void refreshWithoutCookie() throws Exception {
    given(authFacade.refresh(null)).willReturn(LoginResponse.of("a", "r", LoginRole.STUDENT));
    given(jwtUtil.getAccessTokenValidity()).willReturn(Duration.ofMinutes(30));
    given(jwtUtil.getRefreshTokenValidity()).willReturn(Duration.ofDays(14));

    mockMvc.perform(post("/api/v1/oop/auth/refresh"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("logout은 두 토큰 쿠키를 Max-Age=0으로 만료시킨다")
  void logoutExpiresTokenCookies() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/oop/auth/logout"))
        .andExpect(status().isOk())
        .andReturn();

    assertThat(result.getResponse().getContentAsString()).isEqualTo("{\"message\":\"Logout Successfully\"}");
    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .hasSize(2)
        .allMatch(c -> c.contains("Max-Age=0"))
        .allMatch(c -> c.contains("HttpOnly"))
        .allMatch(c -> c.contains("Path=/"))
        .anyMatch(c -> c.startsWith("accessToken="))
        .anyMatch(c -> c.startsWith("refreshToken="));
  }

  @Test
  @DisplayName("logout은 refreshToken 쿠키를 파사드에 넘겨 서버측 폐기를 맡긴다")
  void logoutRevokesStoredToken() throws Exception {
    mockMvc.perform(post("/api/v1/oop/auth/logout").cookie(new Cookie("refreshToken", "refresh-token")))
        .andExpect(status().isOk());

    verify(authFacade).logout("refresh-token");
  }

  @Test
  @DisplayName("logout은 쿠키나 토큰 없이 호출해도 200이다")
  void logoutWithoutCookies() throws Exception {
    mockMvc.perform(post("/api/v1/oop/auth/logout"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("cookie-secure가 켜지면 Secure 속성이 붙는다")
  void secureFlagIsConfigurable() throws Exception {
    mockMvc = MockMvcBuilders.standaloneSetup(new AuthControllerImpl(authFacade, jwtUtil, true)).build();

    MvcResult result = login();

    assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).allMatch(c -> c.contains("Secure"));
  }
}
