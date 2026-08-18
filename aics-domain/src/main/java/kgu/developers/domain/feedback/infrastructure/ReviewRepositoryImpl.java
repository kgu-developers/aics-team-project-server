package kgu.developers.domain.feedback.infrastructure;

import kgu.developers.domain.feedback.domain.Review;
import kgu.developers.domain.feedback.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {
    private final JpaReviewRepository jpaRepository;

    @Override
    public Review save(Review review) {
        return jpaRepository.save(ReviewJpaEntity.toEntity(review)).toDomain();
    }

    @Override
    public Optional<Review> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(ReviewJpaEntity::toDomain);
    }

    @Override
    public Optional<Review> findByVersionId(Long versionId) {
        return jpaRepository.findByVersionIdAndDeletedAtIsNull(versionId)
                .map(ReviewJpaEntity::toDomain);
    }
}
