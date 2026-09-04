package kgu.developers.domain.presentationcontent.domain;

import java.util.List;
import java.util.Optional;

public interface PresentationContentRepository {
    PresentationContent save(PresentationContent presentationContent);

    Optional<PresentationContent> findBySubmissionId(Long submissionId);

    List<PresentationContent> findAllBySubmissionIdIn(List<Long> submissionIds);
}
