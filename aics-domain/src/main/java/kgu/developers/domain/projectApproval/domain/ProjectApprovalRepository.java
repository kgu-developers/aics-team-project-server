package kgu.developers.domain.projectApproval.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectApprovalRepository {
    ProjectApproval save(ProjectApproval projectApproval);

    Optional<ProjectApproval> findById(Long id);

    boolean existsByProjectIdAndUserId(Long projectId, String userId);

    Optional<ProjectApproval> findByProjectIdAndUserId(Long projectId, String userId);

    List<ProjectApproval> findAllByProjectId(Long projectId);

    List<ProjectApproval> findAllByUserId(String userId);

    void deleteById(Long id);
}
