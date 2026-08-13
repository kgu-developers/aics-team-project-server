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
    private String userId;  // 학번

    private Role role;  // 역할
    private Status status;  // 상태

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

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
