package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmationRepository;

public class FakeSubmissionMemberConfirmationRepository implements SubmissionMemberConfirmationRepository {

    private final Map<Long, SubmissionMemberConfirmation> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public SubmissionMemberConfirmation save(SubmissionMemberConfirmation confirmation) {
        Long id = confirmation.getId() != null ? confirmation.getId() : sequence.incrementAndGet();

        SubmissionMemberConfirmation saved = SubmissionMemberConfirmation.builder()
            .id(id)
            .submissionId(confirmation.getSubmissionId())
            .userId(confirmation.getUserId())
            .version(confirmation.getVersion())
            .confirmedFinalReport(confirmation.isConfirmedFinalReport())
            .confirmedArtifacts(confirmation.isConfirmedArtifacts())
            .oneLineReview(confirmation.getOneLineReview())
            .confirmedAt(confirmation.getConfirmedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public List<SubmissionMemberConfirmation> findAllBySubmissionId(Long submissionId) {
        return store.values().stream()
            .filter(confirmation -> confirmation.getSubmissionId().equals(submissionId))
            .toList();
    }

    @Override
    public Optional<SubmissionMemberConfirmation> findBySubmissionIdAndUserId(Long submissionId, String userId) {
        return store.values().stream()
            .filter(confirmation -> confirmation.getSubmissionId().equals(submissionId))
            .filter(confirmation -> confirmation.getUserId().equals(userId))
            .findFirst();
    }
}
