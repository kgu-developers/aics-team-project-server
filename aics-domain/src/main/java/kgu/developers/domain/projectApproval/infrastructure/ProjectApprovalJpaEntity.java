package kgu.developers.domain.projectApproval.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "\"project_approval\"", uniqueConstraints = @UniqueConstraint(name = "uk_project_approval_project_user", columnNames = {
    "project_id", "user_id" }))
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProjectApprovalJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 20)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime approvedAt;

    public void reactivate(LocalDateTime approvedAt) {
        setDeletedAt(null);
        this.approvedAt = approvedAt;
    }

    public ProjectApproval toDomain() {
        return ProjectApproval.builder()
            .id(id)
            .projectId(projectId)
            .userId(userId)
            .approvedAt(approvedAt)
            .createdAt(getCreatedAt())
            .updatedAt(getUpdatedAt())
            .deletedAt(getDeletedAt())
            .build();
    }

    public static ProjectApprovalJpaEntity toEntity(ProjectApproval projectApproval) {
        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.builder()
            .id(projectApproval.getId())
            .projectId(projectApproval.getProjectId())
            .userId(projectApproval.getUserId())
            .approvedAt(projectApproval.getApprovedAt())
            .build();
        entity.createdAt = projectApproval.getCreatedAt();
        entity.setDeletedAt(projectApproval.getDeletedAt());
        return entity;
    }
}
