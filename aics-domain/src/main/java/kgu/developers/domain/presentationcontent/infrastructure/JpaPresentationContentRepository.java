package kgu.developers.domain.presentationcontent.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPresentationContentRepository extends JpaRepository<PresentationContentJpaEntity, Long> {
    Optional<PresentationContentJpaEntity> findBySubmissionId(Long submissionId);

    List<PresentationContentJpaEntity> findAllBySubmissionIdIn(List<Long> submissionIds);
}
