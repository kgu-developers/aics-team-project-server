package kgu.developers.domain.feedback.infrastructure;

import kgu.developers.domain.feedback.domain.MidReportFeedback;
import kgu.developers.domain.feedback.domain.MidReportFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MidReportFeedbackRepositoryImpl implements MidReportFeedbackRepository {
    private final JpaMidReportFeedbackRepository jpaRepository;

    @Override
    public MidReportFeedback save(MidReportFeedback feedback) {
        return jpaRepository.save(MidReportFeedbackJpaEntity.toEntity(feedback)).toDomain();
    }

    @Override
    public Optional<MidReportFeedback> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(MidReportFeedbackJpaEntity::toDomain);
    }

    @Override
    public List<MidReportFeedback> findAllBySubmissionId(Long submissionId) {
        return jpaRepository.findAllBySubmissionIdAndDeletedAtIsNull(submissionId).stream()
                .map(MidReportFeedbackJpaEntity::toDomain)
                .toList();
    }
}
