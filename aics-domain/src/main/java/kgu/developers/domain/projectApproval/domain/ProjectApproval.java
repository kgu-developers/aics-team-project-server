package kgu.developers.domain.projectApproval.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProjectApproval {
    private Long id;

    private Long projectId;  // 프로젝트 식별자
    private String userId;  // 학번

    private LocalDateTime approvedAt;  // 동의일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ProjectApproval create(Long projectId, String userId, LocalDateTime approvedAt) {
        return ProjectApproval.builder()
                .projectId(requireNonNull(projectId, "projectId"))
                .userId(requireNonNull(userId, "userId"))
                .approvedAt(requireNonNull(approvedAt, "approvedAt"))
                .build();
    }

    public void updateApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = requireNonNull(approvedAt, "approvedAt");
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate(LocalDateTime approvedAt) {
        this.deletedAt = null;
        this.approvedAt = requireNonNull(approvedAt, "approvedAt");
    }
}
