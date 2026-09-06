package projectApproval.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalJpaEntity;

class ProjectApprovalJpaEntityTest {

    private ProjectApproval projectApproval(LocalDateTime createdAt, LocalDateTime deletedAt) {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        return ProjectApproval.builder()
            .id(1L)
            .projectId(10L)
            .userId("202012345")
            .approvedAt(approvedAt)
            .createdAt(createdAt)
            .deletedAt(deletedAt)
            .build();
    }

    @Test
    @DisplayName("toEntity는 기존 프로젝트 동의 정보의 생성일과 삭제일을 그대로 옮긴다")
    void toEntityKeepsTimestamps() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);

        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.toEntity(projectApproval(createdAt, deletedAt));

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("toEntity - toDomain 변환은 모든 필드를 보존한다")
    void roundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        ProjectApproval origin = projectApproval(createdAt, null);

        ProjectApproval restored = ProjectApprovalJpaEntity.toEntity(origin).toDomain();

        assertThat(restored.getId()).isEqualTo(origin.getId());
        assertThat(restored.getProjectId()).isEqualTo(origin.getProjectId());
        assertThat(restored.getUserId()).isEqualTo(origin.getUserId());
        assertThat(restored.getApprovedAt()).isEqualTo(origin.getApprovedAt());
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getDeletedAt()).isNull();
    }
}
