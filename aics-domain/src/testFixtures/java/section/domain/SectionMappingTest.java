package section.domain;

import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.exception.InvalidCapacityException;
import kgu.developers.domain.section.exception.InvalidContactVisiblePeriodException;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyPath;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionMappingTest {

    private final CourseJpaEntity course = CourseJpaEntity.builder().id(1L).build();
    private final UserJpaEntity professor = UserJpaEntity.builder().studentNumber("202012345").build();

    @Test
    @DisplayName("Section <-> SectionJpaEntity 왕복 매핑 시 필드가 보존된다")
    void roundTrip() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 2, 9, 0);
        LocalDateTime until = LocalDateTime.of(2026, 6, 20, 18, 0);
        Section section = Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", 40, from, until);

        Section mapped = SectionJpaEntity.toEntity(section, course, professor).toDomain();

        assertThat(mapped).usingRecursiveComparison().isEqualTo(section);
    }

    @Test
    @DisplayName("toEntity는 기존 분반의 생성일을 그대로 옮긴다")
    void toEntityKeepsCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        Section section = Section.builder()
                .id(1L)
                .professorId("202012345")
                .courseId(1L)
                .code("1154")
                .name("월3,4/1154")
                .classTime("월3,4")
                .capacity(40)
                .createdAt(createdAt)
                .build();

        SectionJpaEntity entity = SectionJpaEntity.toEntity(section, course, professor);

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("삭제된 Section은 deletedAt이 엔티티로 전달된다")
    void carriesDeletedAt() {
        Section section = Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", 40, null, null);
        section.delete();

        assertThat(SectionJpaEntity.toEntity(section, course, professor).getDeletedAt())
                .isEqualTo(section.getDeletedAt());
    }

    @Test
    @DisplayName("정원이 음수이면 Section 생성에 실패한다")
    void rejectsNegativeCapacity() {
        assertThatThrownBy(() -> Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", -1, null, null))
                .isInstanceOf(InvalidCapacityException.class);
    }

    @Test
    @DisplayName("연락처 공개 종료가 시작보다 빠르면 Section 생성에 실패한다")
    void rejectsReversedContactVisiblePeriod() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 2, 9, 0);
        LocalDateTime until = from.minusDays(1);

        assertThatThrownBy(() -> Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", 40, from, until))
                .isInstanceOf(InvalidContactVisiblePeriodException.class);
    }

    @Test
    @DisplayName("updateCapacity로 음수 정원을 설정할 수 없다")
    void rejectsNegativeCapacityOnUpdate() {
        Section section = Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", 40, null, null);

        assertThatThrownBy(() -> section.updateCapacity(-1))
                .isInstanceOf(InvalidCapacityException.class);
        assertThat(section.getCapacity()).isEqualTo(40);
    }

    @Test
    @DisplayName("updateContactVisiblePeriod로 연락처 공개 기간을 역전시킬 수 없다")
    void rejectsReversedContactVisiblePeriodOnUpdate() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 2, 9, 0);
        LocalDateTime until = LocalDateTime.of(2026, 6, 20, 18, 0);
        Section section = Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", 40, from, until);

        assertThatThrownBy(() -> section.updateContactVisiblePeriod(until.plusDays(1), until))
                .isInstanceOf(InvalidContactVisiblePeriodException.class);
        assertThatThrownBy(() -> section.updateContactVisiblePeriod(from, from.minusDays(1)))
                .isInstanceOf(InvalidContactVisiblePeriodException.class);
        assertThat(section.getContactVisibleFrom()).isEqualTo(from);
        assertThat(section.getContactVisibleUntil()).isEqualTo(until);
    }

    @Test
    @DisplayName("기존 기간보다 뒤로 통째로 옮기는 수정은 허용된다")
    void allowsShiftingContactVisiblePeriodForward() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 9, 0);
        Section section = Section.create("202012345", 1L, "1154", "월3,4/1154", "월3,4", 40, from, from.plusDays(1));

        section.updateContactVisiblePeriod(from.plusDays(2), from.plusDays(3));

        assertThat(section.getContactVisibleFrom()).isEqualTo(from.plusDays(2));
        assertThat(section.getContactVisibleUntil()).isEqualTo(from.plusDays(3));
    }

    @Test
    @DisplayName("JpaSectionRepository의 파생 쿼리 프로퍼티가 연관 엔티티 식별자로 해석된다")
    void resolvesDerivedQueryProperties() {
        assertThat(PropertyPath.from("courseId", SectionJpaEntity.class).toDotPath())
                .isEqualTo("course.id");
        assertThat(PropertyPath.from("professorStudentNumber", SectionJpaEntity.class).toDotPath())
                .isEqualTo("professor.studentNumber");
    }
}
