package section.domain;

import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyPath;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SectionMappingTest {

    private final CourseJpaEntity course = CourseJpaEntity.builder().id(1L).build();
    private final UserJpaEntity professor = UserJpaEntity.builder().studentNumber("202012345").build();

    @Test
    @DisplayName("Section <-> SectionJpaEntity 왕복 매핑 시 필드가 보존된다")
    void roundTrip() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 2, 9, 0);
        LocalDateTime until = LocalDateTime.of(2026, 6, 20, 18, 0);
        Section section = Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, from, until);

        Section mapped = SectionJpaEntity.toEntity(section, course, professor).toDomain();

        assertThat(mapped).usingRecursiveComparison().isEqualTo(section);
    }

    @Test
    @DisplayName("삭제된 Section은 deletedAt이 엔티티로 전달된다")
    void carriesDeletedAt() {
        Section section = Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, null, null);
        section.delete();

        assertThat(SectionJpaEntity.toEntity(section, course, professor).getDeletedAt())
                .isEqualTo(section.getDeletedAt());
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
