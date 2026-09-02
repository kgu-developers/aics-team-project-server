package user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import kgu.developers.domain.auth.infrastructure.JpaRefreshTokenRepository;
import kgu.developers.domain.user.application.command.UserCommandService;
import static kgu.developers.domain.user.domain.UserGlobalRole.ADMIN;
import static kgu.developers.domain.user.domain.UserGlobalRole.USER;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.DuplicateEmailException;
import kgu.developers.domain.user.exception.DuplicateStudentNumberException;
import kgu.developers.domain.user.exception.InvalidCredentialsException;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JpaRefreshTokenRepository refreshTokenRepository;

  @Mock
  private TokenRevocationStore tokenRevocationStore;

  @InjectMocks
  private UserCommandService commandService;

  private User user() {
    return User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "12345678", USER, "010-1234-6789");
  }

  @Test
  @DisplayName("createUser는 평문이 아닌 해시를 저장한다")
  void createUser() {
    given(passwordEncoder.encode("12345678")).willReturn("hashed");
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    commandService.createUser("202699999", "kgu@kyonggi.ac.kr", "김철수", "12345678", USER,
        "010-1234-6789", false);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
  }

  @Test
  @DisplayName("createUser는 이미 있는 학번이면 DuplicateStudentNumberException을 던진다")
  void createUserWithDuplicateStudentNumber() {
    given(userRepository.existsByStudentNumber("202699999")).willReturn(true);
    given(userRepository.findIncludingDeleted("202699999")).willReturn(Optional.of(user()));

    assertThatThrownBy(() -> commandService.createUser("202699999", "kgu@kyonggi.ac.kr", "김철수",
        "12345678", USER, "010-1234-6789", false))
        .isInstanceOf(DuplicateStudentNumberException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("createUser는 reactivate면 탈퇴 회원을 이력으로 남긴 뒤 새 계정을 만든다")
  void createUserReactivatesWithdrawnStudentNumber() {
    User withdrawn = user();
    withdrawn.delete();
    given(userRepository.existsByStudentNumber("202699999")).willReturn(true);
    given(userRepository.findIncludingDeleted("202699999")).willReturn(Optional.of(withdrawn));
    given(passwordEncoder.encode("12345678")).willReturn("hashed");
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    commandService.createUser("202699999", "kgu@kyonggi.ac.kr", "김철수", "12345678", USER,
        "010-1234-6789", true);

    // 아카이브가 저장보다 먼저 일어나야 옛 계정이 이력으로 남는다
    InOrder inOrder = inOrder(userRepository);
    inOrder.verify(userRepository).archiveAndHardDelete(withdrawn);
    inOrder.verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("createUser는 reactivate여도 탈퇴하지 않은 회원은 덮어쓰지 않는다")
  void createUserRejectsActiveStudentNumberEvenWhenReactivate() {
    given(userRepository.existsByStudentNumber("202699999")).willReturn(true);
    given(userRepository.findIncludingDeleted("202699999")).willReturn(Optional.of(user()));

    assertThatThrownBy(() -> commandService.createUser("202699999", "kgu@kyonggi.ac.kr", "김철수",
        "12345678", USER, "010-1234-6789", true))
        .isInstanceOf(DuplicateStudentNumberException.class);

    verify(userRepository, never()).archiveAndHardDelete(any(User.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateUser는 유저를 수정한 뒤 저장한다")
  void updateUser() {
    User user = user();
    given(passwordEncoder.encode("87654321")).willReturn("hashed");

    commandService.updateUser(user, "new@kyonggi.ac.kr", "김영희", "87654321", ADMIN, "010-9876-5432");

    assertThat(user.getEmail()).isEqualTo("new@kyonggi.ac.kr");
    assertThat(user.getName()).isEqualTo("김영희");
    assertThat(user.getPassword()).isEqualTo("hashed");
    assertThat(user.getGlobalRole()).isEqualTo(ADMIN);
    verify(userRepository).save(user);
    verify(refreshTokenRepository).deleteById("202699999");
  }

  @Test
  @DisplayName("updateUser는 password가 null이면 비밀번호를 그대로 두고 로그아웃시키지도 않는다")
  void updateUserWithoutPassword() {
    User user = user();

    commandService.updateUser(user, "kgu@kyonggi.ac.kr", "김철수", null, USER, "010-9876-5432");

    assertThat(user.getPassword()).isEqualTo("12345678");
    assertThat(user.getPhone()).isEqualTo("010-9876-5432");
    verify(userRepository).save(user);
    verify(refreshTokenRepository, never()).deleteById(any());
    verify(tokenRevocationStore, never()).revokeTokensIssuedBefore(any());
  }

  @Test
  @DisplayName("updateUser는 비밀번호를 안 바꿔도 권한이 바뀌면 토큰을 폐기한다")
  void updateUserWithRoleChangeRevokesTokens() {
    User user = user();

    commandService.updateUser(user, "kgu@kyonggi.ac.kr", "김철수", null, ADMIN, "010-1234-6789");

    verify(refreshTokenRepository).deleteById("202699999");
    verify(tokenRevocationStore).revokeTokensIssuedBefore("202699999");
  }

  @Test
  @DisplayName("updateUser는 다른 회원이 쓰는 이메일이면 DuplicateEmailException을 던진다")
  void updateUserWithDuplicateEmail() {
    User user = user();
    given(userRepository.existsByEmailAndStudentNumberNotAndDeletedAtIsNull("taken@kyonggi.ac.kr", "202699999"))
        .willReturn(true);

    assertThatThrownBy(() -> commandService.updateUser(user, "taken@kyonggi.ac.kr", "김영희", "87654321",
        ADMIN, "010-9876-5432"))
        .isInstanceOf(DuplicateEmailException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updatePassword는 평문이 아닌 해시를 저장한다")
  void updatePassword() {
    User user = user();
    given(passwordEncoder.matches("12345678", "12345678")).willReturn(true);
    given(passwordEncoder.encode("87654321")).willReturn("hashed");

    commandService.updatePassword(user, "12345678", "87654321");

    assertThat(user.getPassword()).isEqualTo("hashed");
    verify(userRepository).save(user);
    verify(refreshTokenRepository).deleteById("202699999");
  }

  @Test
  @DisplayName("updatePassword는 현재 비밀번호가 틀리면 아무것도 바꾸지 않는다")
  void updatePasswordWithWrongCurrentPassword() {
    User user = user();
    given(passwordEncoder.matches("wrong-password", "12345678")).willReturn(false);

    assertThatThrownBy(() -> commandService.updatePassword(user, "wrong-password", "87654321"))
        .isInstanceOf(InvalidCredentialsException.class);

    assertThat(user.getPassword()).isEqualTo("12345678");
    verify(userRepository, never()).save(any(User.class));
    verify(refreshTokenRepository, never()).deleteById(any(String.class));
  }

  @Test
  @DisplayName("updatePassword가 실패하면 refresh token을 지우지 않는다")
  void updatePasswordDoesNotRevokeOnFailure() {
    User user = user();
    given(passwordEncoder.matches("12345678", "12345678")).willReturn(true);
    given(passwordEncoder.encode("87654321")).willThrow(new IllegalStateException("boom"));

    assertThatThrownBy(() -> commandService.updatePassword(user, "12345678", "87654321"))
        .isInstanceOf(IllegalStateException.class);

    verify(refreshTokenRepository, never()).deleteById(any(String.class));
  }

  @Test
  @DisplayName("deleteUser는 삭제 시각을 기록한 뒤 저장하고 refresh token도 지운다 (soft delete)")
  void deleteUser() {
    User user = user();

    commandService.deleteUser(user);

    assertThat(user.getDeletedAt()).isNotNull();
    verify(userRepository).save(user);
    verify(refreshTokenRepository).deleteById("202699999");
  }

  @Test
  @DisplayName("recordLogin은 마지막 로그인 시각을 기록한 뒤 저장한다")
  void recordLogin() {
    User user = user();

    commandService.recordLogin(user);

    assertThat(user.getLastLoginAt()).isNotNull();
    verify(userRepository).save(user);
  }
}
