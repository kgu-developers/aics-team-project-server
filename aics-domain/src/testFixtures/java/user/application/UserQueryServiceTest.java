package user.application;

import static kgu.developers.domain.user.domain.UserGlobalRole.ADMIN;
import static kgu.developers.domain.user.domain.UserGlobalRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.auth.domain.LoginRole;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

  private static final String STUDENT_NUMBER = "202699999";

  @Mock
  private UserRepository userRepository;

  @Mock
  private EnrollmentRepository enrollmentRepository;

  @InjectMocks
  private UserQueryService queryService;

  private void givenUser(UserGlobalRole globalRole) {
    given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(
        User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", globalRole,
            "010-1234-6789")));
  }

  private Enrollment enrollment(Long sectionId, Role role, Status status) {
    return Enrollment.create(sectionId, STUDENT_NUMBER, role, status);
  }

  @Test
  @DisplayName("ADMIN은 수강 정보를 보지 않고 ADMIN을 반환한다")
  void adminRole() {
    givenUser(ADMIN);

    assertThat(queryService.getUserRoleByStudentNumber(STUDENT_NUMBER)).isEqualTo(LoginRole.ADMIN);
  }

  @Test
  @DisplayName("활성 수강 중 조교가 하나라도 있으면 ADMIN을 반환한다")
  void assistantRole() {
    givenUser(USER);
    given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER)).willReturn(List.of(
        enrollment(1L, Role.STUDENT, Status.ACTIVE),
        enrollment(2L, Role.ASSISTANT, Status.ACTIVE)));

    assertThat(queryService.getUserRoleByStudentNumber(STUDENT_NUMBER)).isEqualTo(LoginRole.ADMIN);
  }

  @Test
  @DisplayName("탈퇴한 조교 수강 정보는 무시하고 STUDENT를 반환한다")
  void withdrawnAssistantIsIgnored() {
    givenUser(USER);
    given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER)).willReturn(List.of(
        enrollment(1L, Role.STUDENT, Status.ACTIVE),
        enrollment(2L, Role.ASSISTANT, Status.WITHDRAWN)));

    assertThat(queryService.getUserRoleByStudentNumber(STUDENT_NUMBER)).isEqualTo(LoginRole.STUDENT);
  }

  @Test
  @DisplayName("수강 정보가 없으면 STUDENT를 반환한다")
  void noEnrollment() {
    givenUser(USER);
    given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER)).willReturn(List.of());

    assertThat(queryService.getUserRoleByStudentNumber(STUDENT_NUMBER)).isEqualTo(LoginRole.STUDENT);
  }
}
