package submission.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.infrastructure.SubmissionJpaEntity;

class SubmissionJpaEntityTest {

    @Test
    @DisplayName("fromDomain은 소프트삭제 시각을 그대로 옮긴다")
    void fromDomainKeepsDeletedAt() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        Submission submission = Submission.builder()
                .id(1L)
                .teamId(10L)
                .milestoneId(5L)
                .status(SubmissionStatus.NOT_SUBMITTED)
                .currentVersion(0)
                .deletedAt(deletedAt)
                .build();

        SubmissionJpaEntity entity = SubmissionJpaEntity.fromDomain(submission);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }
}
