package user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import kgu.developers.domain.user.application.command.UserCommandService;
import static kgu.developers.domain.user.domain.UserGlobalRole.PROFESSOR;
import static kgu.developers.domain.user.domain.UserGlobalRole.STUDENT;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.DuplicateEmailException;
import kgu.developers.domain.user.exception.DuplicateStudentNumberException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserCommandService commandService;

  private User user() {
    return User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "12345678", STUDENT, "010-1234-6789");
  }

  @Test
  @DisplayName("createUser는 평문이 아닌 해시를 저장한다")
  void createUser() {
    given(passwordEncoder.encode("12345678")).willReturn("hashed");
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    commandService.createUser("202699999", "kgu@kyonggi.ac.kr", "김철수", "12345678", STUDENT,
        "010-1234-6789");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
  }

  @Test
  @DisplayName("createUser는 이미 있는 학번이면 DuplicateStudentNumberException을 던진다")
  void createUserWithDuplicateStudentNumber() {
    given(userRepository.existsByStudentNumber("202699999")).willReturn(true);

    assertThatThrownBy(() -> commandService.createUser("202699999", "kgu@kyonggi.ac.kr", "김철수",
        "12345678", STUDENT, "010-1234-6789"))
        .isInstanceOf(DuplicateStudentNumberException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updateUser는 유저를 수정한 뒤 저장한다")
  void updateUser() {
    User user = user();
    given(passwordEncoder.encode("87654321")).willReturn("hashed");

    commandService.updateUser(user, "new@kyonggi.ac.kr", "김영희", "87654321", PROFESSOR, "010-9876-5432");

    assertThat(user.getEmail()).isEqualTo("new@kyonggi.ac.kr");
    assertThat(user.getName()).isEqualTo("김영희");
    assertThat(user.getPassword()).isEqualTo("hashed");
    assertThat(user.getGlobalRole()).isEqualTo(PROFESSOR);
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("updateUser는 다른 회원이 쓰는 이메일이면 DuplicateEmailException을 던진다")
  void updateUserWithDuplicateEmail() {
    User user = user();
    given(userRepository.existsByEmailAndStudentNumberNot("taken@kyonggi.ac.kr", "202699999"))
        .willReturn(true);

    assertThatThrownBy(() -> commandService.updateUser(user, "taken@kyonggi.ac.kr", "김영희", "87654321",
        PROFESSOR, "010-9876-5432"))
        .isInstanceOf(DuplicateEmailException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("updatePassword는 평문이 아닌 해시를 저장한다")
  void updatePassword() {
    User user = user();
    given(passwordEncoder.encode("87654321")).willReturn("hashed");

    commandService.updatePassword(user, "87654321");

    assertThat(user.getPassword()).isEqualTo("hashed");
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("deleteUser는 삭제 시각을 기록한 뒤 저장한다 (soft delete)")
  void deleteUser() {
    User user = user();

    commandService.deleteUser(user);

    assertThat(user.getDeletedAt()).isNotNull();
    verify(userRepository).save(user);
  }
}
