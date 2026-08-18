package kgu.developers.domain.section.domain;

import kgu.developers.domain.section.exception.ContactNotVisibleException;
import kgu.developers.domain.section.exception.InvalidCapacityException;
import kgu.developers.domain.section.exception.InvalidContactVisiblePeriodException;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Section {
  private Long id;

  private String professorId; // 교수 학번
  private Long courseId; // 강좌 id

  private String code; // 과목 코드
  private String name; // 분반명
  private String classTime; // 수업시간
  private Integer capacity; // 정원

  private LocalDateTime contactVisibleFrom; // 연락처 공개시작
  private LocalDateTime contactVisibleUntil; // 연락처 공개종료

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public static Section create(String professorId, Long courseId, String code, String name, String classTime,
      Integer capacity, LocalDateTime contactVisibleFrom, LocalDateTime contactVisibleUntil) {
    Section section = Section.builder()
        .professorId(professorId)
        .courseId(courseId)
        .code(code)
        .name(name)
        .classTime(classTime)
        .build();
    section.updateCapacity(capacity);
    section.updateContactVisiblePeriod(contactVisibleFrom, contactVisibleUntil);
    return section;
  }

  public void updateProfessorId(String professorId) {
    this.professorId = professorId;
  }

  public void updateCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public void updateCode(String code) {
    this.code = code;
  }

  public void updateName(String name) {
    this.name = name;
  }

  public void updateClassTime(String classTime) {
    this.classTime = classTime;
  }

  public void updateCapacity(Integer capacity) {
    if (capacity != null && capacity < 0) {
      throw new InvalidCapacityException();
    }
    this.capacity = capacity;
  }

  public void updateContactVisiblePeriod(LocalDateTime contactVisibleFrom, LocalDateTime contactVisibleUntil) {
    if (contactVisibleFrom != null && contactVisibleUntil != null
        && contactVisibleUntil.isBefore(contactVisibleFrom)) {
      throw new InvalidContactVisiblePeriodException();
    }
    this.contactVisibleFrom = contactVisibleFrom;
    this.contactVisibleUntil = contactVisibleUntil;
  }

  /** 공개 시작이 없으면 아직 공개 전, 공개 종료가 없으면 기한 없이 공개. 경계는 포함한다. */
  public void validateContactVisible(LocalDateTime at) {
    if (contactVisibleFrom == null || at.isBefore(contactVisibleFrom)) {
      throw new ContactNotVisibleException();
    }
    if (contactVisibleUntil != null && at.isAfter(contactVisibleUntil)) {
      throw new ContactNotVisibleException();
    }
  }

  public void delete() {
    deletedAt = LocalDateTime.now();
  }
}
