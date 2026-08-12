package kgu.developers.domain.feedback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaReviewRepository extends JpaRepository<ReviewJpaEntity, Long> {
    Optional<ReviewJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<ReviewJpaEntity> findByVersionIdAndDeletedAtIsNull(Long versionId);
}
