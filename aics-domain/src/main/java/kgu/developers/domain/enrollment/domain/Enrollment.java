package kgu.developers.domain.enrollment.domain;

import lombok.*;

import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Enrollment {
  private Long id;

  private Long sectionId; // 분반 식별자
  private String userId; // 학번

  private Role role; // 역할
  private Status status; // 상태
  private String grade; // 학년. 팀 편성과 수명이 다른 수강생 속성이라 TeamMember가 아닌 여기에 둔다

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public static Enrollment create(Long sectionId, String userId, Role role, Status status) {
    return Enrollment.builder()
        .sectionId(sectionId)
        .userId(userId)
        .role(role)
        .status(status)
        .build();
  }

  public void updateSectionId(Long sectionId) {
    this.sectionId = sectionId;
  }

  public void updateUserId(String userId) {
    this.userId = userId;
  }

  public void updateRole(Role role) {
    this.role = role;
  }

  public void updateStatus(Status status) {
    this.status = status;
  }

  // null은 "값 없음"이지 "지우기"가 아니다. 학년 열이 없는 구형 양식이나 빈 셀로
  // 명단을 다시 올려도 기존 값이 날아가지 않게 한다.
  public void updateGrade(String grade) {
    if (grade != null) {
      this.grade = grade;
    }
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
  }

  public void reactivate(Role role) {
    this.deletedAt = null;
    this.role = role;
    this.status = Status.ACTIVE;
  }

  public boolean isActiveStudent() {
    return this.status == Status.ACTIVE && this.role == Role.STUDENT;
  }

  public boolean isActiveAssistant() {
    return this.status == Status.ACTIVE && this.role == Role.ASSISTANT;
  }
}
