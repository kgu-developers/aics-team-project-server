package config;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import jakarta.servlet.http.Cookie;
import kgu.developers.admin.config.JwtCookieAuthenticationFilter;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.user.application.UserAdminFacade;
import kgu.developers.admin.user.presentation.UserAdminControllerImpl;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.globalutils.jwt.JwtUtil;

@WebMvcTest
@Import({SecurityConfig.class, JwtCookieAuthenticationFilter.class, JwtUtil.class, CorsConfig.class,
    UserAdminControllerImpl.class})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
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
  @WithMockUser(roles = "STUDENT")
  void notAdmin() throws Exception {
    mockMvc.perform(get(ADMIN_URL))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("관리자 요청은 200을 응답한다")
  @WithMockUser(roles = "PROFESSOR")
  void admin() throws Exception {
    given(userAdminFacade.getAllUsers()).willReturn(UserAdminListResponse.from(List.of()));

    mockMvc.perform(get(ADMIN_URL))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("PROFESSOR 역할의 accessToken 쿠키를 담으면 200을 응답한다")
  void adminAccessTokenCookie() throws Exception {
    given(userAdminFacade.getAllUsers()).willReturn(UserAdminListResponse.from(List.of()));

    mockMvc.perform(get(ADMIN_URL).cookie(accessTokenCookie("PROFESSOR")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("STUDENT 역할의 accessToken 쿠키는 403을 응답한다")
  void studentAccessTokenCookie() throws Exception {
    mockMvc.perform(get(ADMIN_URL).cookie(accessTokenCookie("STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("위조된 accessToken 쿠키는 401을 응답한다")
  void forgedAccessTokenCookie() throws Exception {
    mockMvc.perform(get(ADMIN_URL).cookie(new Cookie("accessToken", "not-a-jwt")))
        .andExpect(status().isUnauthorized());
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
