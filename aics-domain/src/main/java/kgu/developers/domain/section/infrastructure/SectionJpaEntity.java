package kgu.developers.domain.section.infrastructure;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "section")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SectionJpaEntity extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private Long id;

  @ManyToOne(fetch = LAZY, optional = false)
  @JoinColumn(name = "professor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_section_professor"))
  private UserJpaEntity professor;

  @ManyToOne(fetch = LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_section_course"))
  private CourseJpaEntity course;

  @Column(nullable = false, length = 32)
  private String code;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(nullable = false, length = 128)
  private String classTime;

  @Column(nullable = false)
  @Positive
  private Integer capacity;

  @Column
  private LocalDateTime contactVisibleFrom;

  @Column
  private LocalDateTime contactVisibleUntil;

  public Section toDomain() {
    return Section.builder()
        .id(id)
        // 프록시여도 식별자 접근은 초기화를 유발하지 않는다
        .professorId(professor.getStudentNumber())
        .courseId(course.getId())
        .code(code)
        .name(name)
        .classTime(classTime)
        .capacity(capacity)
        .contactVisibleFrom(contactVisibleFrom)
        .contactVisibleUntil(contactVisibleUntil)
        .createdAt(getCreatedAt())
        .updatedAt(getUpdatedAt())
        .deletedAt(getDeletedAt())
        .build();
  }

  /** course/professor를 함께 fetch 한 조회에서만 호출한다 */
  public SectionDetail toDetail() {
    return new SectionDetail(toDomain(), course.toDomain(), professor.toDomain());
  }

  public static SectionJpaEntity toEntity(Section section, CourseJpaEntity course, UserJpaEntity professor) {
    SectionJpaEntity entity = SectionJpaEntity.builder()
        .id(section.getId())
        .professor(professor)
        .course(course)
        .code(section.getCode())
        .name(section.getName())
        .classTime(section.getClassTime())
        .capacity(section.getCapacity())
        .contactVisibleFrom(section.getContactVisibleFrom())
        .contactVisibleUntil(section.getContactVisibleUntil())
        .build();
    entity.createdAt = section.getCreatedAt();
    entity.setDeletedAt(section.getDeletedAt());
    return entity;
  }
}
