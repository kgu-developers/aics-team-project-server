package kgu.developers.domain.projectApproval.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectApprovalRepository {
    ProjectApproval save(ProjectApproval projectApproval);

    Optional<ProjectApproval> findById(Long id);

    /**
     * 해당 리비전의 동의 이력을 삭제된 것까지 포함해 잠근 채로 조회한다.
     */
    Optional<ProjectApproval> findIncludingDeleted(Long projectId, String userId, long proposalRevision);

    List<ProjectApproval> findAllByProjectId(Long projectId);

    List<ProjectApproval> findAllByProjectIdAndProposalRevision(Long projectId, long proposalRevision);

    ApprovalCount countApprovalsByTeamMembers(Long projectId, Long teamId, long proposalRevision);

    void deleteById(Long id);

    void deleteAllByProjectId(Long projectId);
}
