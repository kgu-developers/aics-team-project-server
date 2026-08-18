package kgu.developers.domain.feedback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaMidReportFeedbackRepository extends JpaRepository<MidReportFeedbackJpaEntity, Long> {
    Optional<MidReportFeedbackJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<MidReportFeedbackJpaEntity> findAllBySubmissionIdAndDeletedAtIsNull(Long submissionId);
}
