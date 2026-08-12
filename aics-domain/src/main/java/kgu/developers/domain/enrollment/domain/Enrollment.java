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

    private Long sectionId;  // 분반 식별자
    private String studentNumber;  // 학번

    private RoleType role;  // 역할
    private StatusType status;  // 상태

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Enrollment create(Long sectionId, String studentNumber, RoleType role, StatusType status) {
        return Enrollment.builder()
                .sectionId(sectionId)
                .studentNumber(studentNumber)
                .role(role)
                .status(status)
                .build();
    }

    public void updateSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public void updateStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public void updateRole(RoleType role) {
        this.role = role;
    }

    public void updateStatus(StatusType status) {
        this.status = status;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
