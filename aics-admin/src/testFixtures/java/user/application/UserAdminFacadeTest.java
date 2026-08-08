package user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.user.application.UserAdminFacade;
import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.request.UserAdminUpdateRequest;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.application.query.UserQueryService;
import static kgu.developers.domain.user.domain.UserGlobalRole.USER;

import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class UserAdminFacadeTest {

  @Mock
  private UserCommandService userCommandService;

  @Mock
  private UserQueryService userQueryService;

  @InjectMocks
  private UserAdminFacade userAdminFacade;

  private static final String STUDENT_NUMBER = "202699999";

  private final UserAdminRequest request =
      new UserAdminRequest(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", USER, "010-1234-6789");

  private final UserAdminUpdateRequest updateRequest =
      new UserAdminUpdateRequest("new@kyonggi.ac.kr", "김영희", "87654321", USER, "010-9876-5432");

  private User user() {
    return User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", USER, "010-1234-6789");
  }

  @Test
  @DisplayName("createUser는 요청 값을 순서대로 커맨드 서비스에 넘기고 학번을 응답한다")
  void createUser() {
    given(userCommandService.createUser(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", USER,
        "010-1234-6789")).willReturn(STUDENT_NUMBER);

    assertThat(userAdminFacade.createUser(request).studentNumber()).isEqualTo(STUDENT_NUMBER);

    verify(userCommandService).createUser(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", USER,
        "010-1234-6789");
  }

  @Test
  @DisplayName("updateUser는 조회한 유저를 커맨드 서비스에 넘긴다")
  void updateUser() {
    User user = user();
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user);

    userAdminFacade.updateUser(STUDENT_NUMBER, updateRequest);

    verify(userCommandService).updateUser(user, "new@kyonggi.ac.kr", "김영희", "87654321", USER,
        "010-9876-5432");
  }

  @Test
  @DisplayName("deleteUser는 조회한 유저를 커맨드 서비스에 넘긴다")
  void deleteUser() {
    User user = user();
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user);

    userAdminFacade.deleteUser(STUDENT_NUMBER);

    verify(userCommandService).deleteUser(user);
  }

  @Test
  @DisplayName("getUserByStudentNumber는 비밀번호를 뺀 응답 DTO로 감싸 반환한다")
  void getUserByStudentNumber() {
    User user = user();
    given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(user);

    assertThat(userAdminFacade.getUserByStudentNumber(STUDENT_NUMBER)).isEqualTo(UserAdminResponse.from(user));
  }

  @Test
  @DisplayName("getAllUsers는 목록을 응답으로 감싸 반환한다")
  void getAllUsers() {
    List<User> users = List.of(user());
    given(userQueryService.getAllUsers()).willReturn(users);

    assertThat(userAdminFacade.getAllUsers().contents())
        .isEqualTo(users.stream().map(UserAdminResponse::from).toList());
  }
}
