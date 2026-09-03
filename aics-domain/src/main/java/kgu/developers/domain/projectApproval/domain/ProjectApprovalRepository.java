package kgu.developers.domain.projectApproval.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectApprovalRepository {
    ProjectApproval save(ProjectApproval projectApproval);

    Optional<ProjectApproval> findById(Long id);

    boolean existsByProjectIdAndUserIdAndProposalRevision(Long projectId, String userId, long proposalRevision);

    List<ProjectApproval> findAllByProjectId(Long projectId);

    List<ProjectApproval> findAllByProjectIdAndProposalRevision(Long projectId, long proposalRevision);

    ApprovalCount countApprovalsByTeamMembers(Long projectId, Long teamId, long proposalRevision);

    void deleteById(Long id);

    void deleteAllByProjectId(Long projectId);
}
