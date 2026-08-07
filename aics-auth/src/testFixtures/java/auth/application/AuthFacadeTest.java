package auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.auth.api.application.AuthFacade;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.domain.user.application.query.UserQueryService;
import static kgu.developers.domain.user.domain.UserGlobalRole.STUDENT;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.InvalidCredentialsException;
import kgu.developers.domain.user.exception.InvalidTokenException;
import kgu.developers.domain.user.exception.UserNotFoundException;
import io.jsonwebtoken.JwtException;
import kgu.developers.globalutils.jwt.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

  @Mock
  private UserQueryService userQueryService;

  @Mock
  private JwtUtil jwtUtil;

  @InjectMocks
  private AuthFacade userFacade;

  private static final String STUDENT_NUMBER = "202699999";
  private static final String PASSWORD = "12345678";

  private User user() {
    return User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", PASSWORD, STUDENT, "010-1234-6789");
  }

  @Test
  @DisplayName("login은 학번과 비밀번호가 맞으면 accessToken과 refreshToken을 반환한다")
  void login() {
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(jwtUtil.createAccessToken(STUDENT_NUMBER, "STUDENT")).willReturn("access-token");
    given(jwtUtil.createRefreshToken(STUDENT_NUMBER)).willReturn("refresh-token");

    LoginResponse response = userFacade.login(new LoginRequest(STUDENT_NUMBER, PASSWORD));

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  @DisplayName("refresh는 refreshToken이 유효하면 토큰을 새로 발급한다")
  void refresh() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(jwtUtil.createAccessToken(STUDENT_NUMBER, "STUDENT")).willReturn("new-access-token");
    given(jwtUtil.createRefreshToken(STUDENT_NUMBER)).willReturn("new-refresh-token");

    LoginResponse response = userFacade.refresh("refresh-token");

    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
  }

  @Test
  @DisplayName("refresh는 쿠키에 refreshToken이 없으면 InvalidTokenException을 던진다")
  void refreshWithoutToken() {
    assertThatThrownBy(() -> userFacade.refresh(null))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  @DisplayName("refresh는 토큰 검증에 실패하면 InvalidTokenException을 던진다")
  void refreshWithInvalidToken() {
    given(jwtUtil.parseRefreshTokenSubject("broken")).willThrow(new JwtException("invalid"));

    assertThatThrownBy(() -> userFacade.refresh("broken"))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  @DisplayName("refresh는 토큰이 유효해도 삭제된 회원이면 재발급하지 않는다")
  void refreshForDeletedUser() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willThrow(new UserNotFoundException());

    assertThatThrownBy(() -> userFacade.refresh("refresh-token"))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  @DisplayName("login은 비밀번호가 틀리면 InvalidCredentialsException을 던진다")
  void loginWithWrongPassword() {
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());

    assertThatThrownBy(() -> userFacade.login(new LoginRequest(STUDENT_NUMBER, "wrong-password")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("login은 없는 학번도 비밀번호 오류와 같은 예외로 응답한다 (계정 존재 여부 노출 방지)")
  void loginWithUnknownStudentNumber() {
    given(userQueryService.getUserByStudentNumber("000000000")).willThrow(new UserNotFoundException());

    assertThatThrownBy(() -> userFacade.login(new LoginRequest("000000000", PASSWORD)))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
