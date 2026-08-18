package feedback.infrastructure;

import kgu.developers.domain.feedback.domain.MidReportFeedback;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.feedback.domain.Review;
import kgu.developers.domain.feedback.domain.ReviewResultStatus;
import kgu.developers.domain.feedback.infrastructure.JpaMidReportFeedbackRepository;
import kgu.developers.domain.feedback.infrastructure.JpaRequiredArtifactRepository;
import kgu.developers.domain.feedback.infrastructure.JpaReviewRepository;
import kgu.developers.domain.feedback.infrastructure.MidReportFeedbackJpaEntity;
import kgu.developers.domain.feedback.infrastructure.MidReportFeedbackRepositoryImpl;
import kgu.developers.domain.feedback.infrastructure.RequiredArtifactJpaEntity;
import kgu.developers.domain.feedback.infrastructure.RequiredArtifactRepositoryImpl;
import kgu.developers.domain.feedback.infrastructure.ReviewJpaEntity;
import kgu.developers.domain.feedback.infrastructure.ReviewRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackRepositoryImplTest {
    @Mock
    private JpaRequiredArtifactRepository jpaRequiredArtifactRepository;

    @Mock
    private JpaReviewRepository jpaReviewRepository;

    @Mock
    private JpaMidReportFeedbackRepository jpaMidReportFeedbackRepository;

    @Test
    @DisplayName("필수 산출물 저장소 어댑터는 저장 결과를 도메인으로 반환한다")
    void saveRequiredArtifact() {
        RequiredArtifactRepositoryImpl repository = new RequiredArtifactRepositoryImpl(jpaRequiredArtifactRepository);
        RequiredArtifact artifact = RequiredArtifact.create(1L, RequiredArtifactType.FILE, "중간보고서", true, "pdf", 20);
        given(jpaRequiredArtifactRepository.save(any(RequiredArtifactJpaEntity.class)))
                .willReturn(RequiredArtifactJpaEntity.toEntity(
                        RequiredArtifact.restore(1L, 1L, RequiredArtifactType.FILE, "중간보고서", true, "pdf", 20, null, null, null)
                ));

        RequiredArtifact saved = repository.save(artifact);

        assertThat(saved.getId()).isEqualTo(1L);
        ArgumentCaptor<RequiredArtifactJpaEntity> captor = ArgumentCaptor.forClass(RequiredArtifactJpaEntity.class);
        verify(jpaRequiredArtifactRepository).save(captor.capture());
        assertThat(captor.getValue().getMilestoneId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("필수 산출물 저장소 어댑터는 마일스톤별 목록을 조회한다")
    void findRequiredArtifactsByMilestone() {
        RequiredArtifactRepositoryImpl repository = new RequiredArtifactRepositoryImpl(jpaRequiredArtifactRepository);
        given(jpaRequiredArtifactRepository.findAllByMilestoneIdAndDeletedAtIsNull(1L))
                .willReturn(List.of(RequiredArtifactJpaEntity.toEntity(
                        RequiredArtifact.restore(1L, 1L, RequiredArtifactType.FILE, "중간보고서", true, "pdf", 20, null, null, null)
                )));

        List<RequiredArtifact> artifacts = repository.findAllByMilestoneId(1L);

        assertThat(artifacts).extracting(RequiredArtifact::getLabel).containsExactly("중간보고서");
    }

    @Test
    @DisplayName("리뷰 저장소 어댑터는 id로 삭제되지 않은 리뷰를 조회한다")
    void findReviewById() {
        ReviewRepositoryImpl repository = new ReviewRepositoryImpl(jpaReviewRepository);
        given(jpaReviewRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(ReviewJpaEntity.toEntity(
                        Review.restore(1L, 2L, "20260001", ReviewResultStatus.APPROVED, "좋습니다", null, null, null)
                )));

        Optional<Review> found = repository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getResultStatus()).isEqualTo(ReviewResultStatus.APPROVED);
    }

    @Test
    @DisplayName("리뷰 저장소 어댑터는 버전별 공식 리뷰를 단건으로 조회한다")
    void findReviewByVersion() {
        ReviewRepositoryImpl repository = new ReviewRepositoryImpl(jpaReviewRepository);
        given(jpaReviewRepository.findByVersionIdAndDeletedAtIsNull(2L))
                .willReturn(Optional.of(ReviewJpaEntity.toEntity(
                        Review.restore(1L, 2L, "20260001", ReviewResultStatus.REVISION_REQUESTED, "보완 필요", null, null, null)
                )));

        Optional<Review> review = repository.findByVersionId(2L);

        assertThat(review).isPresent();
        assertThat(review.get().getReviewerId()).isEqualTo("20260001");
    }

    @Test
    @DisplayName("중간보고서 피드백 저장소 어댑터는 저장 결과를 도메인으로 반환한다")
    void saveMidReportFeedback() {
        MidReportFeedbackRepositoryImpl repository = new MidReportFeedbackRepositoryImpl(jpaMidReportFeedbackRepository);
        MidReportFeedback feedback = MidReportFeedback.create(3L, "20260002", "현장 요약", "추가 의견", "수정 안내");
        given(jpaMidReportFeedbackRepository.save(any(MidReportFeedbackJpaEntity.class)))
                .willReturn(MidReportFeedbackJpaEntity.toEntity(
                        MidReportFeedback.restore(1L, 3L, "20260002", "현장 요약", "추가 의견", "수정 안내", null, null, null)
                ));

        MidReportFeedback saved = repository.save(feedback);

        assertThat(saved.getId()).isEqualTo(1L);
        ArgumentCaptor<MidReportFeedbackJpaEntity> captor = ArgumentCaptor.forClass(MidReportFeedbackJpaEntity.class);
        verify(jpaMidReportFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getSubmissionId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("중간보고서 피드백 저장소 어댑터는 제출물별 목록을 조회한다")
    void findMidReportFeedbacksBySubmission() {
        MidReportFeedbackRepositoryImpl repository = new MidReportFeedbackRepositoryImpl(jpaMidReportFeedbackRepository);
        given(jpaMidReportFeedbackRepository.findAllBySubmissionIdAndDeletedAtIsNull(3L))
                .willReturn(List.of(MidReportFeedbackJpaEntity.toEntity(
                        MidReportFeedback.restore(1L, 3L, "20260002", "현장 요약", null, null, null, null, null)
                )));

        List<MidReportFeedback> feedbacks = repository.findAllBySubmissionId(3L);

        assertThat(feedbacks).extracting(MidReportFeedback::getAuthorId).containsExactly("20260002");
    }
}
