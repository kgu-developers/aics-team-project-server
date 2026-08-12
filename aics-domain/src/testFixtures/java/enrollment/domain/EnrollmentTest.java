package enrollment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.RoleType;
import kgu.developers.domain.enrollment.domain.StatusType;

class EnrollmentTest {

  @Test
  @DisplayName("create는 전달받은 값으로 수강 정보를 생성한다")
  void create() {
    Enrollment enrollment = Enrollment.create(1L, "202012345", RoleType.STUDENT, StatusType.ACTIVE);

    assertThat(enrollment.getSectionId()).isEqualTo(1L);
    assertThat(enrollment.getStudentNumber()).isEqualTo("202012345");
    assertThat(enrollment.getRole()).isEqualTo(RoleType.STUDENT);
    assertThat(enrollment.getStatus()).isEqualTo(StatusType.ACTIVE);
    assertThat(enrollment.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("update 메서드들은 각 필드를 변경한다")
  void update() {
    Enrollment enrollment = Enrollment.create(1L, "202012345", RoleType.STUDENT, StatusType.ACTIVE);

    enrollment.updateSectionId(2L);
    enrollment.updateStudentNumber("202154321");
    enrollment.updateRole(RoleType.ASSISTANT);
    enrollment.updateStatus(StatusType.WITHDRAWN);

    assertThat(enrollment.getSectionId()).isEqualTo(2L);
    assertThat(enrollment.getStudentNumber()).isEqualTo("202154321");
    assertThat(enrollment.getRole()).isEqualTo(RoleType.ASSISTANT);
    assertThat(enrollment.getStatus()).isEqualTo(StatusType.WITHDRAWN);
  }

  @Test
  @DisplayName("delete는 삭제 시각을 기록한다")
  void delete() {
    Enrollment enrollment = Enrollment.create(1L, "202012345", RoleType.STUDENT, StatusType.ACTIVE);

    enrollment.delete();

    assertThat(enrollment.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("역할과 상태 enum은 한글 설명을 가진다")
  void enumDescriptions() {
    assertThat(RoleType.STUDENT.getDescription()).isEqualTo("학생");
    assertThat(RoleType.ASSISTANT.getDescription()).isEqualTo("조교");

    assertThat(StatusType.ACTIVE.getDescription()).isEqualTo("활성");
    assertThat(StatusType.WITHDRAWN.getDescription()).isEqualTo("탈퇴");
  }
}
