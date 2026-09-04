package submission.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.infrastructure.SubmissionVersionJpaEntity;

class SubmissionVersionJpaEntityTest {

    @Test
    @DisplayName("fromDomain은 소프트삭제 시각을 그대로 옮긴다")
    void fromDomainKeepsDeletedAt() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        SubmissionVersion version = SubmissionVersion.builder()
                .id(1L)
                .submissionId(10L)
                .version(1)
                .submittedBy("202412345")
                .submittedAt(LocalDateTime.now())
                .deletedAt(deletedAt)
                .build();

        SubmissionVersionJpaEntity entity = SubmissionVersionJpaEntity.fromDomain(version);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }
}
