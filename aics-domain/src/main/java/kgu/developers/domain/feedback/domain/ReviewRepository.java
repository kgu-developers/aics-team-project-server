package kgu.developers.domain.feedback.domain;

import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);

    Optional<Review> findById(Long id);

    Optional<Review> findByVersionId(Long versionId);
}
