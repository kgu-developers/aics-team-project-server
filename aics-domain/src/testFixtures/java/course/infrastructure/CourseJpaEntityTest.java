package course.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;

class CourseJpaEntityTest {

  @Test
  @DisplayName("toEntity는 기존 강좌의 생성일과 삭제일을 그대로 옮긴다")
  void toEntityKeepsTimestamps() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
    Course course = Course.builder()
        .id(1L)
        .name("객체지향프로그래밍")
        .year(2026)
        .semester(SemesterType.FALL)
        .status(StatusType.DRAFT)
        .createdAt(createdAt)
        .deletedAt(deletedAt)
        .build();

    CourseJpaEntity entity = CourseJpaEntity.toEntity(course);

    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
  }
}
