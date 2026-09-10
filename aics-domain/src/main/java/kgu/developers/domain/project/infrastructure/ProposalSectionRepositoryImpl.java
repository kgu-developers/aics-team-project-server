package kgu.developers.domain.project.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionRepository;
import kgu.developers.domain.project.domain.ProposalSectionType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProposalSectionRepositoryImpl implements ProposalSectionRepository {

    private final JpaProposalSectionRepository jpaProposalSectionRepository;

    @Override
    public ProposalSection save(ProposalSection proposalSection) {
        return jpaProposalSectionRepository.saveAndFlush(ProposalSectionJpaEntity.toEntity(proposalSection)).toDomain();
    }

    @Override
    public List<ProposalSection> findAllByProjectId(Long projectId) {
        return jpaProposalSectionRepository.findAllByProjectIdOrderByIdAsc(projectId).stream()
            .map(ProposalSectionJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<ProposalSection> findByProjectIdAndType(Long projectId, ProposalSectionType type) {
        return jpaProposalSectionRepository.findByProjectIdAndType(projectId, type)
            .map(ProposalSectionJpaEntity::toDomain);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        jpaProposalSectionRepository.deleteAllByProjectId(projectId);
    }
}
