package feedback.infrastructure;

import kgu.developers.domain.feedback.domain.MidReportFeedback;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.feedback.domain.Review;
import kgu.developers.domain.feedback.domain.ReviewResultStatus;
import kgu.developers.domain.feedback.infrastructure.MidReportFeedbackJpaEntity;
import kgu.developers.domain.feedback.infrastructure.RequiredArtifactJpaEntity;
import kgu.developers.domain.feedback.infrastructure.ReviewJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackJpaEntityTest {

    @Test
    @DisplayName("필수 산출물 JPA entity는 domain과 왕복 매핑된다")
    void requiredArtifactRoundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 12, 9, 0);
        RequiredArtifact artifact = RequiredArtifact.restore(
                1L,
                2L,
                RequiredArtifactType.FILE,
                "중간보고서",
                true,
                "pdf, docx",
                20,
                createdAt,
                null,
                null
        );

        RequiredArtifact mapped = RequiredArtifactJpaEntity.toEntity(artifact).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getMilestoneId()).isEqualTo(2L);
        assertThat(mapped.getType()).isEqualTo(RequiredArtifactType.FILE);
        assertThat(mapped.getLabel()).isEqualTo("중간보고서");
        assertThat(mapped.isRequired()).isTrue();
        assertThat(mapped.getAllowedExtensions()).isEqualTo("pdf, docx");
        assertThat(mapped.getMaxFileSizeMb()).isEqualTo(20);
        assertThat(mapped.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("필수 산출물 JPA entity는 파일 설정이 없는 유형도 왕복 매핑한다")
    void requiredArtifactRoundTripWithoutFileOptions() {
        RequiredArtifact artifact = RequiredArtifact.restore(
                1L,
                2L,
                RequiredArtifactType.CHEERPJ_RUN,
                "실행 확인",
                true,
                null,
                null,
                null,
                null,
                null
        );

        RequiredArtifact mapped = RequiredArtifactJpaEntity.toEntity(artifact).toDomain();

        assertThat(mapped.getType()).isEqualTo(RequiredArtifactType.CHEERPJ_RUN);
        assertThat(mapped.getAllowedExtensions()).isNull();
        assertThat(mapped.getMaxFileSizeMb()).isNull();
    }

    @Test
    @DisplayName("리뷰 JPA entity는 domain과 왕복 매핑된다")
    void reviewRoundTrip() {
        Review review = Review.restore(
                1L,
                2L,
                "20260001",
                ReviewResultStatus.APPROVED,
                "좋습니다",
                null,
                null,
                null
        );

        Review mapped = ReviewJpaEntity.toEntity(review).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getVersionId()).isEqualTo(2L);
        assertThat(mapped.getReviewerId()).isEqualTo("20260001");
        assertThat(mapped.getResultStatus()).isEqualTo(ReviewResultStatus.APPROVED);
        assertThat(mapped.getComment()).isEqualTo("좋습니다");
    }

    @Test
    @DisplayName("중간보고서 피드백 JPA entity는 domain과 왕복 매핑된다")
    void midReportFeedbackRoundTrip() {
        MidReportFeedback feedback = MidReportFeedback.restore(
                1L,
                2L,
                "20260002",
                "현장 요약",
                "추가 의견",
                "수정 안내",
                null,
                null,
                null
        );

        MidReportFeedback mapped = MidReportFeedbackJpaEntity.toEntity(feedback).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getSubmissionId()).isEqualTo(2L);
        assertThat(mapped.getAuthorId()).isEqualTo("20260002");
        assertThat(mapped.getOnsiteFeedbackSummary()).isEqualTo("현장 요약");
        assertThat(mapped.getProfessorAdditionalFeedback()).isEqualTo("추가 의견");
        assertThat(mapped.getRevisionNote()).isEqualTo("수정 안내");
    }
}
