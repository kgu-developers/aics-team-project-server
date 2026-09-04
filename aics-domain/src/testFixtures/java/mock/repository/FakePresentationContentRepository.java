package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.presentationcontent.domain.PresentationContent;
import kgu.developers.domain.presentationcontent.domain.PresentationContentRepository;

public class FakePresentationContentRepository implements PresentationContentRepository {

    private final Map<Long, PresentationContent> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public PresentationContent save(PresentationContent presentationContent) {
        Long id = presentationContent.getId() != null ? presentationContent.getId() : sequence.incrementAndGet();

        PresentationContent saved = PresentationContent.builder()
            .id(id)
            .submissionId(presentationContent.getSubmissionId())
            .introText(presentationContent.getIntroText())
            .features(presentationContent.getFeatures())
            .screens(presentationContent.getScreens())
            .youtubeUrl(presentationContent.getYoutubeUrl())
            .createdAt(presentationContent.getCreatedAt())
            .updatedAt(presentationContent.getUpdatedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<PresentationContent> findBySubmissionId(Long submissionId) {
        return store.values().stream()
            .filter(content -> content.getSubmissionId().equals(submissionId))
            .findFirst();
    }

    @Override
    public List<PresentationContent> findAllBySubmissionIdIn(List<Long> submissionIds) {
        return store.values().stream()
            .filter(content -> submissionIds.contains(content.getSubmissionId()))
            .toList();
    }
}
