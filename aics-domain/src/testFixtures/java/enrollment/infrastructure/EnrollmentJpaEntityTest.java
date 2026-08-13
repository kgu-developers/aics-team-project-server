package enrollment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.enrollment.infrastructure.EnrollmentJpaEntity;

class EnrollmentJpaEntityTest {

  private Enrollment enrollment(LocalDateTime createdAt, LocalDateTime deletedAt) {
    return Enrollment.builder()
        .id(1L)
        .sectionId(10L)
        .studentNumber("202012345")
        .role(Role.STUDENT)
        .status(Status.ACTIVE)
        .createdAt(createdAt)
        .deletedAt(deletedAt)
        .build();
  }

  @Test
  @DisplayName("toEntity는 기존 수강 정보의 생성일과 삭제일을 그대로 옮긴다")
  void toEntityKeepsTimestamps() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);

    EnrollmentJpaEntity entity = EnrollmentJpaEntity.toEntity(enrollment(createdAt, deletedAt));

    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
  }

  @Test
  @DisplayName("toEntity - toDomain 변환은 모든 필드를 보존한다")
  void roundTrip() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
    Enrollment origin = enrollment(createdAt, null);

    Enrollment restored = EnrollmentJpaEntity.toEntity(origin).toDomain();

    assertThat(restored.getId()).isEqualTo(origin.getId());
    assertThat(restored.getSectionId()).isEqualTo(origin.getSectionId());
    assertThat(restored.getStudentNumber()).isEqualTo(origin.getStudentNumber());
    assertThat(restored.getRole()).isEqualTo(origin.getRole());
    assertThat(restored.getStatus()).isEqualTo(origin.getStatus());
    assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
    assertThat(restored.getDeletedAt()).isNull();
  }
}
