package feedback.domain;

import kgu.developers.domain.feedback.domain.MidReportFeedback;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.feedback.domain.Review;
import kgu.developers.domain.feedback.domain.ReviewResultStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackDomainTest {

    @Test
    @DisplayName("필수 산출물 생성은 앞뒤 공백을 제거하고 전달받은 값을 보관한다")
    void createRequiredArtifact() {
        RequiredArtifact artifact = RequiredArtifact.create(1L, RequiredArtifactType.FILE, " 중간보고서 ", true, " pdf, docx ", 20);

        assertThat(artifact.getId()).isNull();
        assertThat(artifact.getMilestoneId()).isEqualTo(1L);
        assertThat(artifact.getType()).isEqualTo(RequiredArtifactType.FILE);
        assertThat(artifact.getLabel()).isEqualTo("중간보고서");
        assertThat(artifact.isRequired()).isTrue();
        assertThat(artifact.getAllowedExtensions()).isEqualTo("pdf, docx");
        assertThat(artifact.getMaxFileSizeMb()).isEqualTo(20);
    }

    @Test
    @DisplayName("필수 산출물 생성은 파일 계열이 아닌 유형에서 파일 크기와 확장자를 비워 둘 수 있다")
    void createRequiredArtifactWithoutFileOptions() {
        RequiredArtifact artifact = RequiredArtifact.create(1L, RequiredArtifactType.CHEERPJ_RUN, "실행 확인", true, null, null);

        assertThat(artifact.getType()).isEqualTo(RequiredArtifactType.CHEERPJ_RUN);
        assertThat(artifact.getAllowedExtensions()).isNull();
        assertThat(artifact.getMaxFileSizeMb()).isNull();
    }

    @Test
    @DisplayName("필수 산출물 생성은 잘못된 필수값을 거부한다")
    void createRequiredArtifactValidation() {
        assertThatThrownBy(() -> RequiredArtifact.create(null, RequiredArtifactType.FILE, "중간보고서", true, "pdf", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("마일스톤 id는 양수여야 합니다.");
        assertThatThrownBy(() -> RequiredArtifact.create(1L, null, "중간보고서", true, "pdf", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 산출물 유형은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> RequiredArtifact.create(1L, RequiredArtifactType.FILE, " ", true, "pdf", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 산출물 이름은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> RequiredArtifact.create(1L, RequiredArtifactType.FILE, "가".repeat(101), true, "pdf", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 산출물 이름은 100자를 초과할 수 없습니다.");
        assertThatThrownBy(() -> RequiredArtifact.create(1L, RequiredArtifactType.FILE, "중간보고서", true, "pdf", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 파일 크기는 양수여야 합니다.");
    }

    @Test
    @DisplayName("필수 산출물 유형은 CheerpJ 실행 유형을 포함한다")
    void requiredArtifactTypeIncludesCheerpjRun() {
        assertThat(RequiredArtifactType.CHEERPJ_RUN.getDescription()).isEqualTo("CheerpJ 실행");
    }

    @Test
    @DisplayName("리뷰 생성은 리뷰어 학번과 의견을 정규화하고 결과 상태를 보관한다")
    void createReview() {
        Review review = Review.create(2L, " 20260001 ", ReviewResultStatus.REVISION_REQUESTED, " 보완 필요 ");

        assertThat(review.getId()).isNull();
        assertThat(review.getVersionId()).isEqualTo(2L);
        assertThat(review.getReviewerId()).isEqualTo("20260001");
        assertThat(review.getResultStatus()).isEqualTo(ReviewResultStatus.REVISION_REQUESTED);
        assertThat(review.getComment()).isEqualTo("보완 필요");
    }

    @Test
    @DisplayName("리뷰 생성은 PRD 밖 점수 없이 최소 결과 상태만 사용한다")
    void reviewResultStatusDescriptions() {
        assertThat(ReviewResultStatus.APPROVED.getDescription()).isEqualTo("승인");
        assertThat(ReviewResultStatus.FEEDBACK_PROVIDED.getDescription()).isEqualTo("피드백 제공");
        assertThat(ReviewResultStatus.REVISION_REQUESTED.getDescription()).isEqualTo("수정 요청");
    }

    @Test
    @DisplayName("리뷰 생성은 잘못된 필수값을 거부한다")
    void createReviewValidation() {
        assertThatThrownBy(() -> Review.create(0L, "20260001", ReviewResultStatus.APPROVED, "좋습니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("버전 id는 양수여야 합니다.");
        assertThatThrownBy(() -> Review.create(1L, " ", ReviewResultStatus.APPROVED, "좋습니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰어 학번은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> Review.create(1L, "1".repeat(17), ReviewResultStatus.APPROVED, "좋습니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰어 학번은 16자를 초과할 수 없습니다.");
        assertThatThrownBy(() -> Review.create(1L, "20260001", null, "좋습니다"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("피드백 결과 상태는 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("중간보고서 피드백 생성은 작성자 학번과 피드백 문구를 정규화한다")
    void createMidReportFeedback() {
        MidReportFeedback feedback = MidReportFeedback.create(3L, " 20260002 ", " 현장 요약 ", " 추가 의견 ", " 수정 안내 ");

        assertThat(feedback.getId()).isNull();
        assertThat(feedback.getSubmissionId()).isEqualTo(3L);
        assertThat(feedback.getAuthorId()).isEqualTo("20260002");
        assertThat(feedback.getOnsiteFeedbackSummary()).isEqualTo("현장 요약");
        assertThat(feedback.getProfessorAdditionalFeedback()).isEqualTo("추가 의견");
        assertThat(feedback.getRevisionNote()).isEqualTo("수정 안내");
    }

    @Test
    @DisplayName("중간보고서 피드백 생성은 잘못된 필수값을 거부한다")
    void createMidReportFeedbackValidation() {
        assertThatThrownBy(() -> MidReportFeedback.create(null, "20260002", "현장 요약", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제출물 id는 양수여야 합니다.");
        assertThatThrownBy(() -> MidReportFeedback.create(1L, " ", "현장 요약", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("작성자 학번은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> MidReportFeedback.create(1L, "1".repeat(17), "현장 요약", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("작성자 학번은 16자를 초과할 수 없습니다.");
        assertThatThrownBy(() -> MidReportFeedback.create(1L, "20260002", " ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현장 피드백 요약은 비어 있을 수 없습니다.");
        assertThatThrownBy(() -> MidReportFeedback.create(1L, "20260002", "가".repeat(2001), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현장 피드백 요약은 2000자를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("복원은 모든 엔티티의 id가 없으면 예외를 던진다")
    void restoreRequiresId() {
        assertThatThrownBy(() -> RequiredArtifact.restore(null, 1L, RequiredArtifactType.FILE, "중간보고서", true, "pdf", 20, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 산출물 id는 양수여야 합니다.");
        assertThatThrownBy(() -> Review.restore(null, 1L, "20260001", ReviewResultStatus.APPROVED, "좋습니다", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰 id는 양수여야 합니다.");
        assertThatThrownBy(() -> MidReportFeedback.restore(null, 1L, "20260002", "현장 요약", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("중간보고서 피드백 id는 양수여야 합니다.");
    }
}
