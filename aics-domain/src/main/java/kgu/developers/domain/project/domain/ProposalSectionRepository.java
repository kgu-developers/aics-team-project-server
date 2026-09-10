package kgu.developers.domain.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProposalSectionRepository {
    ProposalSection save(ProposalSection proposalSection);

    List<ProposalSection> findAllByProjectId(Long projectId);

    Optional<ProposalSection> findByProjectIdAndType(Long projectId, ProposalSectionType type);

    void deleteAllByProjectId(Long projectId);
}
