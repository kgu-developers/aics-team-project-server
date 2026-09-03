package kgu.developers.domain.presentationcontent.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.presentationcontent.domain.PresentationContent;
import kgu.developers.domain.presentationcontent.domain.PresentationContentRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PresentationContentRepositoryImpl implements PresentationContentRepository {
    private final JpaPresentationContentRepository jpaRepository;

    @Override
    public PresentationContent save(PresentationContent presentationContent) {
        PresentationContentJpaEntity entity = PresentationContentJpaEntity.fromDomain(presentationContent);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<PresentationContent> findBySubmissionId(Long submissionId) {
        return jpaRepository.findBySubmissionId(submissionId)
                .map(PresentationContentJpaEntity::toDomain);
    }

    @Override
    public List<PresentationContent> findAllBySubmissionIdIn(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllBySubmissionIdIn(submissionIds).stream()
                .map(PresentationContentJpaEntity::toDomain)
                .toList();
    }
}
