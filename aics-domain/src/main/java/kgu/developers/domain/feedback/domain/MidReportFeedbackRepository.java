package kgu.developers.domain.feedback.domain;

import java.util.List;
import java.util.Optional;

public interface MidReportFeedbackRepository {
    MidReportFeedback save(MidReportFeedback feedback);

    Optional<MidReportFeedback> findById(Long id);

    List<MidReportFeedback> findAllBySubmissionId(Long submissionId);
}
