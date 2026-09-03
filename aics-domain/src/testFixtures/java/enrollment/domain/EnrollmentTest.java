package enrollment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import kgu.developers.domain.enrollment.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Status;

class EnrollmentTest {

  @Test
  @DisplayName("create는 전달받은 값으로 수강 정보를 생성한다")
  void create() {
    Enrollment enrollment = Enrollment.create(1L, "202012345", Role.STUDENT, Status.ACTIVE);

    assertThat(enrollment.getSectionId()).isEqualTo(1L);
    assertThat(enrollment.getUserId()).isEqualTo("202012345");
    assertThat(enrollment.getRole()).isEqualTo(Role.STUDENT);
    assertThat(enrollment.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(enrollment.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("update 메서드들은 각 필드를 변경한다")
  void update() {
    Enrollment enrollment = Enrollment.create(1L, "202012345", Role.STUDENT, Status.ACTIVE);

    enrollment.updateSectionId(2L);
    enrollment.updateUserId("202154321");
    enrollment.updateRole(Role.ASSISTANT);
    enrollment.updateStatus(Status.WITHDRAWN);

    assertThat(enrollment.getSectionId()).isEqualTo(2L);
    assertThat(enrollment.getUserId()).isEqualTo("202154321");
    assertThat(enrollment.getRole()).isEqualTo(Role.ASSISTANT);
    assertThat(enrollment.getStatus()).isEqualTo(Status.WITHDRAWN);
  }

  @Test
  @DisplayName("delete는 삭제 시각을 기록한다")
  void delete() {
    Enrollment enrollment = Enrollment.create(1L, "202012345", Role.STUDENT, Status.ACTIVE);

    enrollment.delete();

    assertThat(enrollment.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("역할과 상태 enum은 한글 설명을 가진다")
  void enumDescriptions() {
    assertThat(Role.STUDENT.getDescription()).isEqualTo("학생");
    assertThat(Role.ASSISTANT.getDescription()).isEqualTo("조교");

    assertThat(Status.ACTIVE.getDescription()).isEqualTo("활성");
    assertThat(Status.WITHDRAWN.getDescription()).isEqualTo("탈퇴");
  }

  @Test
  @DisplayName("isActiveStudent는 ACTIVE 상태의 STUDENT인 경우에만 true를 반환한다")
  void isActiveStudent() {
    Enrollment activeStudent = Enrollment.create(1L, "202012345", Role.STUDENT, Status.ACTIVE);
    Enrollment withdrawnStudent = Enrollment.create(1L, "202012345", Role.STUDENT, Status.WITHDRAWN);
    Enrollment activeAssistant = Enrollment.create(1L, "202012345", Role.ASSISTANT, Status.ACTIVE);

    assertThat(activeStudent.isActiveStudent()).isTrue();
    assertThat(withdrawnStudent.isActiveStudent()).isFalse();
    assertThat(activeAssistant.isActiveStudent()).isFalse();
  }

  @Test
  @DisplayName("isActiveAssistant는 ACTIVE 상태의 ASSISTANT인 경우에만 true를 반환한다")
  void isActiveAssistant() {
    Enrollment activeAssistant = Enrollment.create(1L, "202012345", Role.ASSISTANT, Status.ACTIVE);
    Enrollment withdrawnAssistant = Enrollment.create(1L, "202012345", Role.ASSISTANT, Status.WITHDRAWN);
    Enrollment activeStudent = Enrollment.create(1L, "202012345", Role.STUDENT, Status.ACTIVE);

    assertThat(activeAssistant.isActiveAssistant()).isTrue();
    assertThat(withdrawnAssistant.isActiveAssistant()).isFalse();
    assertThat(activeStudent.isActiveAssistant()).isFalse();
  }
}
