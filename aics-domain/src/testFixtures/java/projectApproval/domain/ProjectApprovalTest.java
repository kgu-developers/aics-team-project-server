package projectApproval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.projectApproval.domain.ProjectApproval;

class ProjectApprovalTest {

    @Test
    @DisplayName("create는 전달받은 값으로 프로젝트 동의 정보를 생성한다")
    void create() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        ProjectApproval projectApproval = ProjectApproval.create(1L, "202012345", approvedAt);

        assertThat(projectApproval.getProjectId()).isEqualTo(1L);
        assertThat(projectApproval.getUserId()).isEqualTo("202012345");
        assertThat(projectApproval.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(projectApproval.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("updateApprovedAt은 동의일을 변경하되 식별자는 그대로 둔다")
    void update() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        ProjectApproval projectApproval = ProjectApproval.create(1L, "202012345", approvedAt);

        LocalDateTime newApprovedAt = LocalDateTime.of(2026, 1, 20, 11, 0);
        projectApproval.updateApprovedAt(newApprovedAt);

        assertThat(projectApproval.getApprovedAt()).isEqualTo(newApprovedAt);
        assertThat(projectApproval.getProjectId()).isEqualTo(1L);
        assertThat(projectApproval.getUserId()).isEqualTo("202012345");
    }

    @Test
    @DisplayName("delete는 삭제 시각을 기록한다")
    void delete() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        ProjectApproval projectApproval = ProjectApproval.create(1L, "202012345", approvedAt);

        projectApproval.delete();

        assertThat(projectApproval.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("delete는 호출할 때마다 삭제 시각을 갱신한다")
    void deleteUpdatesTimestamp() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        ProjectApproval projectApproval = ProjectApproval.create(1L, "202012345", approvedAt);

        projectApproval.delete();
        LocalDateTime firstDeletedAt = projectApproval.getDeletedAt();

        projectApproval.delete();
        LocalDateTime secondDeletedAt = projectApproval.getDeletedAt();

        assertThat(secondDeletedAt).isAfterOrEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("updateApprovedAt는 null이면 예외를 발생시킨다")
    void updateApprovedAtThrowsOnNull() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        ProjectApproval projectApproval = ProjectApproval.create(1L, "202012345", approvedAt);

        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> projectApproval.updateApprovedAt(null)
        );
    }
}
