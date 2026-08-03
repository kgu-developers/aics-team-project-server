package course.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;

class CourseTest {

  @Test
  @DisplayName("create는 전달받은 값으로 강좌를 생성한다")
  void create() {
    Course course = Course.create("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);

    assertThat(course.getName()).isEqualTo("객체지향프로그래밍");
    assertThat(course.getYear()).isEqualTo(2026);
    assertThat(course.getSemester()).isEqualTo(SemesterType.FALL);
    assertThat(course.getStatus()).isEqualTo(StatusType.DRAFT);
    assertThat(course.getDeleted_at()).isNull();
  }

  @Test
  @DisplayName("update 메서드들은 각 필드를 변경한다")
  void update() {
    Course course = Course.create("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);

    course.updateName("자료구조");
    course.updateYear(2027);
    course.updateSemester(SemesterType.SPRING);
    course.updateStatus(StatusType.ACTIVE);

    assertThat(course.getName()).isEqualTo("자료구조");
    assertThat(course.getYear()).isEqualTo(2027);
    assertThat(course.getSemester()).isEqualTo(SemesterType.SPRING);
    assertThat(course.getStatus()).isEqualTo(StatusType.ACTIVE);
  }

  @Test
  @DisplayName("delete는 삭제 시각을 기록한다")
  void delete() {
    Course course = Course.create("객체지향프로그래밍", 2026, SemesterType.FALL, StatusType.DRAFT);

    course.delete();

    assertThat(course.getDeleted_at()).isNotNull();
  }

  @Test
  @DisplayName("학기와 상태 enum은 한글 설명을 가진다")
  void enumDescriptions() {
    assertThat(SemesterType.SPRING.getDescription()).isEqualTo("봄");
    assertThat(SemesterType.SUMMER.getDescription()).isEqualTo("여름");
    assertThat(SemesterType.FALL.getDescription()).isEqualTo("가을");
    assertThat(SemesterType.WINTER.getDescription()).isEqualTo("겨울");

    assertThat(StatusType.DRAFT.getDescription()).isEqualTo("임시 저장");
    assertThat(StatusType.ACTIVE.getDescription()).isEqualTo("활성");
    assertThat(StatusType.ARCHIVED.getDescription()).isEqualTo("보관");
  }
}
