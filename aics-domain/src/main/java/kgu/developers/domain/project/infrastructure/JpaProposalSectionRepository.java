package kgu.developers.domain.project.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kgu.developers.domain.project.domain.ProposalSectionType;

public interface JpaProposalSectionRepository extends JpaRepository<ProposalSectionJpaEntity, Long> {
    List<ProposalSectionJpaEntity> findAllByProjectIdOrderByIdAsc(Long projectId);

    Optional<ProposalSectionJpaEntity> findByProjectIdAndType(Long projectId, ProposalSectionType type);

    void deleteAllByProjectId(Long projectId);
}
