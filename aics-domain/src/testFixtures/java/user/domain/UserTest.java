package user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static kgu.developers.domain.user.domain.UserGlobalRole.PROFESSOR;
import static kgu.developers.domain.user.domain.UserGlobalRole.STUDENT;

import kgu.developers.domain.user.domain.User;

class UserTest {

  private User user() {
    return User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "12345678", STUDENT, "010-1234-6789");
  }

  @Test
  @DisplayName("create는 전달받은 값으로 유저를 생성한다")
  void create() {
    User user = user();

    assertThat(user.getStudent_number()).isEqualTo("202699999");
    assertThat(user.getEmail()).isEqualTo("kgu@kyonggi.ac.kr");
    assertThat(user.getName()).isEqualTo("김철수");
    assertThat(user.getPassword()).isEqualTo("12345678");
    assertThat(user.getGlobal_role()).isEqualTo(STUDENT);
    assertThat(user.getPhone()).isEqualTo("010-1234-6789");
    assertThat(user.getDeleted_at()).isNull();
  }

  @Test
  @DisplayName("update 메서드들은 각 필드를 변경한다")
  void update() {
    User user = user();

    user.updateName("김영희");
    user.updatePassword("87654321");
    user.updateGlobalRole(PROFESSOR);
    user.updatePhone("010-9876-5432");

    assertThat(user.getName()).isEqualTo("김영희");
    assertThat(user.getPassword()).isEqualTo("87654321");
    assertThat(user.getGlobal_role()).isEqualTo(PROFESSOR);
    assertThat(user.getPhone()).isEqualTo("010-9876-5432");
  }

  @Test
  @DisplayName("update는 학번과 이메일을 바꾸지 않는다")
  void updateKeepsIdentity() {
    User user = user();

    user.updateName("김영희");

    assertThat(user.getStudent_number()).isEqualTo("202699999");
    assertThat(user.getEmail()).isEqualTo("kgu@kyonggi.ac.kr");
  }

  @Test
  @DisplayName("delete는 삭제 시각을 기록한다")
  void delete() {
    User user = user();

    user.delete();

    assertThat(user.getDeleted_at()).isNotNull();
  }
}
