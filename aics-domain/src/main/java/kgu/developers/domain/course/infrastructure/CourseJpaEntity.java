package kgu.developers.domain.course.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "\"course\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class CourseJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, length = 16)
    @Enumerated(STRING)
    private SemesterType semester;

    @Column(nullable = false, length = 16)
    @Enumerated(STRING)
    private StatusType status;

    public Course toDomain() {
        return Course.builder()
                .id(id)
                .name(name)
                .year(year)
                .semester(semester)
                .status(status)
                .created_at(getCreatedAt())
                .updated_at(getUpdatedAt())
                .deleted_at(getDeletedAt())
                .build();
    }

    public static CourseJpaEntity toEntity(Course course) {
        CourseJpaEntity entity = CourseJpaEntity.builder()
                .id(course.getId())
                .name(course.getName())
                .year(course.getYear())
                .semester(course.getSemester())
                .status(course.getStatus())
                .build();
        entity.setDeletedAt(course.getDeleted_at());
        return entity;
    }
}
