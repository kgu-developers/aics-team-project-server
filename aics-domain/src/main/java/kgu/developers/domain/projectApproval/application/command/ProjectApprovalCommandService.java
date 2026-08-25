package kgu.developers.domain.projectApproval.application.command;

import java.time.LocalDateTime;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApprovalCommandService {

    private final ProjectApprovalRepository projectApprovalRepository;
    private final ProjectRepository projectRepository;

    public void approve(Long projectId, String userId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(ProjectNotFoundException::new);
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        long proposalRevision = project.getProposalRevision();
        if (projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(projectId, userId, proposalRevision)) {
            throw new DuplicateProjectApprovalException();
        }
        try {
            projectApprovalRepository.save(ProjectApproval.create(projectId, userId, proposalRevision, LocalDateTime.now()));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateProjectApprovalException();
        }
    }
}
