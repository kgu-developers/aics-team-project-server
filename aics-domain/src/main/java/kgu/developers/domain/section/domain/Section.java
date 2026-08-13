package kgu.developers.domain.section.domain;

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
    if (capacity != null && capacity < 0) {
      throw new InvalidCapacityException();
    }
    if (contactVisibleFrom != null && contactVisibleUntil != null && contactVisibleUntil.isBefore(contactVisibleFrom)) {
      throw new InvalidContactVisiblePeriodException();
    }
    return Section.builder()
        .professorId(professorId)
        .courseId(courseId)
        .code(code)
        .name(name)
        .classTime(classTime)
        .capacity(capacity)
        .contactVisibleFrom(contactVisibleFrom)
        .contactVisibleUntil(contactVisibleUntil)
        .build();
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
    this.capacity = capacity;
  }

  public void updateContactVisibleFrom(LocalDateTime contactVisibleFrom) {
    this.contactVisibleFrom = contactVisibleFrom;
  }

  public void updateContactVisibleUntil(LocalDateTime contactVisibleUntil) {
    this.contactVisibleUntil = contactVisibleUntil;
  }

  public void delete() {
    deletedAt = LocalDateTime.now();
  }
}
