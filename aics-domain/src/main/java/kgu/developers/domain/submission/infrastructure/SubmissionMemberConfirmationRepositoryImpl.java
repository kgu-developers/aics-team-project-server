package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmationRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubmissionMemberConfirmationRepositoryImpl implements SubmissionMemberConfirmationRepository {
    private final JpaSubmissionMemberConfirmationRepository jpaRepository;

    @Override
    public SubmissionMemberConfirmation save(SubmissionMemberConfirmation confirmation) {
        SubmissionMemberConfirmationJpaEntity entity = SubmissionMemberConfirmationJpaEntity.fromDomain(confirmation);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<SubmissionMemberConfirmation> findAllBySubmissionId(Long submissionId) {
        return jpaRepository.findAllBySubmissionId(submissionId).stream()
                .map(SubmissionMemberConfirmationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SubmissionMemberConfirmation> findBySubmissionIdAndUserId(Long submissionId, String userId) {
        return jpaRepository.findBySubmissionIdAndUserId(submissionId, userId)
                .map(SubmissionMemberConfirmationJpaEntity::toDomain);
    }
}
