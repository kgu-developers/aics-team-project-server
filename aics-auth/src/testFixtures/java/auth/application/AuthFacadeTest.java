package auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import kgu.developers.auth.api.application.AuthFacade;
import kgu.developers.auth.api.application.RefreshTokenStore;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.domain.auth.domain.LoginRole;
import kgu.developers.domain.user.application.query.UserQueryService;
import static kgu.developers.domain.user.domain.UserGlobalRole.USER;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.InvalidCredentialsException;
import kgu.developers.domain.user.exception.InvalidTokenException;
import kgu.developers.domain.user.exception.UserNotFoundException;
import io.jsonwebtoken.JwtException;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

  @Mock
  private UserQueryService userQueryService;

  @Mock
  private JwtUtil jwtUtil;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private RefreshTokenStore refreshTokenStore;

  @Mock
  private TokenRevocationStore tokenRevocationStore;

  @InjectMocks
  private AuthFacade userFacade;

  private static final String STUDENT_NUMBER = "202699999";
  private static final String PASSWORD = "12345678";

  private User user() {
    return User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", PASSWORD, USER, "010-1234-6789");
  }

  @Test
  @DisplayName("login은 학번과 비밀번호가 맞으면 accessToken과 refreshToken을 반환한다")
  void login() {
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(passwordEncoder.matches(PASSWORD, PASSWORD)).willReturn(true);
    given(userQueryService.getUserRoleByStudentNumber(STUDENT_NUMBER)).willReturn(LoginRole.STUDENT);
    given(jwtUtil.createAccessToken(STUDENT_NUMBER, "USER")).willReturn("access-token");
    given(jwtUtil.createRefreshToken(STUDENT_NUMBER)).willReturn("refresh-token");

    LoginResponse response = userFacade.login(new LoginRequest(STUDENT_NUMBER, PASSWORD));

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.role()).isEqualTo(LoginRole.STUDENT);
    verify(refreshTokenStore).save(STUDENT_NUMBER, "refresh-token");
  }

  @Test
  @DisplayName("refresh는 refreshToken이 유효하면 토큰을 새로 발급한다")
  void refresh() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(userQueryService.getUserRoleByStudentNumber(STUDENT_NUMBER)).willReturn(LoginRole.STUDENT);
    given(jwtUtil.createAccessToken(STUDENT_NUMBER, "USER")).willReturn("new-access-token");
    given(jwtUtil.createRefreshToken(STUDENT_NUMBER)).willReturn("new-refresh-token");
    given(refreshTokenStore.replace(STUDENT_NUMBER, "refresh-token", "new-refresh-token"))
        .willReturn(true);

    LoginResponse response = userFacade.refresh("refresh-token");

    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    assertThat(response.role()).isEqualTo(LoginRole.STUDENT);
  }

  @Test
  @DisplayName("refresh는 이미 사용한 refreshToken이면 재발급하지 않는다")
  void refreshWithUsedToken() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(refreshTokenStore.replace(any(), any(), any())).willReturn(false);

    assertThatThrownBy(() -> userFacade.refresh("refresh-token"))
        .isInstanceOf(InvalidTokenException.class);

    verify(refreshTokenStore, never()).save(any(), any());
  }

  @Test
  @DisplayName("refresh는 서버에 저장된 토큰과 다르면 재발급하지 않고, 저장된 토큰도 건드리지 않는다")
  void refreshWithRotatedToken() {
    given(jwtUtil.parseRefreshTokenSubject("old-refresh-token")).willReturn(STUDENT_NUMBER);
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(jwtUtil.createRefreshToken(STUDENT_NUMBER)).willReturn("new-refresh-token");
    given(refreshTokenStore.replace(STUDENT_NUMBER, "old-refresh-token", "new-refresh-token"))
        .willReturn(false);

    assertThatThrownBy(() -> userFacade.refresh("old-refresh-token"))
        .isInstanceOf(InvalidTokenException.class);

    // 탈취본/재사용 요청이 정상 세션의 토큰을 지우면 안 된다.
    verify(refreshTokenStore, never()).save(any(), any());
    verify(refreshTokenStore, never()).deleteIfMatches(any(), any());
  }

  @Test
  @DisplayName("refresh는 회전 경쟁에서 밀려 낙관적 락이 깨지면 재발급하지 않는다")
  void refreshLosingOptimisticLock() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user());
    given(jwtUtil.createRefreshToken(STUDENT_NUMBER)).willReturn("new-refresh-token");
    given(refreshTokenStore.replace(STUDENT_NUMBER, "refresh-token", "new-refresh-token"))
        .willThrow(new OptimisticLockingFailureException("conflict"));

    assertThatThrownBy(() -> userFacade.refresh("refresh-token"))
        .isInstanceOf(InvalidTokenException.class);

    verify(refreshTokenStore, never()).save(any(), any());
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

    verify(refreshTokenStore, never()).replace(any(), any(), any());
  }

  @Test
  @DisplayName("logout은 제시된 refreshToken을 대조한 뒤 accessToken까지 무효화한다")
  void logout() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(refreshTokenStore.deleteIfMatches(STUDENT_NUMBER, "refresh-token")).willReturn(true);

    userFacade.logout("refresh-token");

    // 학번만으로 지우면 회전된 옛 토큰으로도 남의 활성 세션을 끊을 수 있다
    verify(refreshTokenStore).deleteIfMatches(STUDENT_NUMBER, "refresh-token");
    verify(tokenRevocationStore).revokeTokensIssuedBefore(STUDENT_NUMBER);
  }

  @Test
  @DisplayName("logout은 쿠키가 없거나 토큰이 깨졌으면 아무것도 폐기하지 않는다")
  void logoutWithoutValidToken() {
    given(jwtUtil.parseRefreshTokenSubject("broken")).willThrow(new JwtException("invalid"));

    userFacade.logout(null);
    userFacade.logout("broken");

    verify(refreshTokenStore, never()).deleteIfMatches(any(), any());
    verify(tokenRevocationStore, never()).revokeTokensIssuedBefore(any());
  }

  @Test
  @DisplayName("logout은 토큰이 현재 것이 아니면 accessToken도 무효화하지 않고, 그래도 성공한다")
  void logoutWithRotatedTokenStillSucceeds() {
    given(jwtUtil.parseRefreshTokenSubject("rotated-out-token")).willReturn(STUDENT_NUMBER);
    given(refreshTokenStore.deleteIfMatches(STUDENT_NUMBER, "rotated-out-token")).willReturn(false);

    assertThatCode(() -> userFacade.logout("rotated-out-token")).doesNotThrowAnyException();

    // 대조 없이 무효화하면 옛 토큰만으로 남의 accessToken을 죽일 수 있다
    verify(tokenRevocationStore, never()).revokeTokensIssuedBefore(any());
  }

  @Test
  @DisplayName("logout은 동시 요청으로 낙관적 락이 깨져도 성공한다")
  void logoutLosingOptimisticLock() {
    given(jwtUtil.parseRefreshTokenSubject("refresh-token")).willReturn(STUDENT_NUMBER);
    given(refreshTokenStore.deleteIfMatches(STUDENT_NUMBER, "refresh-token"))
        .willThrow(new OptimisticLockingFailureException("conflict"));

    assertThatCode(() -> userFacade.logout("refresh-token")).doesNotThrowAnyException();

    verify(tokenRevocationStore, never()).revokeTokensIssuedBefore(any());
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
