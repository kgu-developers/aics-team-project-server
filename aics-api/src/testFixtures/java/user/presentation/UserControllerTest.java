package user.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import kgu.developers.api.config.SecurityConfig;
import kgu.developers.api.user.application.UserFacade;
import kgu.developers.api.user.presentation.UserControllerImpl;
import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@WebMvcTest
@Import({SecurityConfig.class, JwtCookieAuthenticationFilter.class, JwtUtil.class,
    UserControllerImpl.class})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com"
})
class UserControllerTest {

  private static final String STUDENT_NUMBER = "202699999";
  private static final String OTHER_STUDENT_NUMBER = "202611111";
  private static final String BODY = """
      {"currentPassword":"12345678","password":"87654321"}""";

  @SpringBootConfiguration
  static class TestApp {
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtUtil jwtUtil;

  @MockitoBean
  private UserFacade userFacade;

  @MockitoBean
  private TokenRevocationStore tokenRevocationStore;

  private Cookie accessTokenCookie(String studentNumber) {
    return new Cookie("accessToken", jwtUtil.createAccessToken(studentNumber, "USER"));
  }

  @Test
  @DisplayName("본인 학번이면 비밀번호를 변경한다")
  void updateOwnPassword() throws Exception {
    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", STUDENT_NUMBER)
            .with(csrf())
            .cookie(accessTokenCookie(STUDENT_NUMBER))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isOk());

    verify(userFacade).updateUserPassword(STUDENT_NUMBER,
        new UserUpdateRequest("12345678", "87654321"));
  }

  @Test
  @DisplayName("CSRF 토큰이 없으면 403을 응답하고 변경하지 않는다")
  void updateWithoutCsrfToken() throws Exception {
    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", STUDENT_NUMBER)
            .cookie(accessTokenCookie(STUDENT_NUMBER))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isForbidden());

    verify(userFacade, never()).updateUserPassword(any(), any());
  }

  @Test
  @DisplayName("현재 비밀번호가 빠지면 400을 응답하고 변경하지 않는다")
  void updateWithoutCurrentPassword() throws Exception {
    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", STUDENT_NUMBER)
            .with(csrf())
            .cookie(accessTokenCookie(STUDENT_NUMBER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"password":"87654321"}"""))
        .andExpect(status().isBadRequest());

    verify(userFacade, never()).updateUserPassword(any(), any());
  }

  @Test
  @DisplayName("새 비밀번호가 UTF-8 72바이트면 변경한다")
  void updateWithMaxByteLengthPassword() throws Exception {
    String password = "가".repeat(24); // 72 bytes

    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", STUDENT_NUMBER)
            .with(csrf())
            .cookie(accessTokenCookie(STUDENT_NUMBER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"currentPassword":"12345678","password":"%s"}""".formatted(password)))
        .andExpect(status().isOk());

    verify(userFacade).updateUserPassword(STUDENT_NUMBER,
        new UserUpdateRequest("12345678", password));
  }

  @Test
  @DisplayName("새 비밀번호가 UTF-8 72바이트를 넘으면 400을 응답하고 변경하지 않는다")
  void updateWithTooManyBytesPassword() throws Exception {
    String password = "가".repeat(25); // 75 bytes, 25 chars so @Size(max = 64) is not triggered

    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", STUDENT_NUMBER)
            .with(csrf())
            .cookie(accessTokenCookie(STUDENT_NUMBER))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"currentPassword":"12345678","password":"%s"}""".formatted(password)))
        .andExpect(status().isBadRequest());

    verify(userFacade, never()).updateUserPassword(any(), any());
  }

  @Test
  @DisplayName("다른 사람 학번이면 403을 응답하고 변경하지 않는다")
  void updateOthersPassword() throws Exception {
    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", OTHER_STUDENT_NUMBER)
            .with(csrf())
            .cookie(accessTokenCookie(STUDENT_NUMBER))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isForbidden());

    verify(userFacade, never()).updateUserPassword(any(), any());
  }

  @Test
  @DisplayName("토큰이 없으면 401을 응답한다")
  void updateWithoutToken() throws Exception {
    mockMvc.perform(put("/api/v1/oop/users/{studentNumber}/password", STUDENT_NUMBER)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isUnauthorized());

    verify(userFacade, never()).updateUserPassword(any(), any());
  }
}
