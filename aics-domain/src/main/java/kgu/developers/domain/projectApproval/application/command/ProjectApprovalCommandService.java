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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApprovalCommandService {

    private static final String UNIQUE_APPROVAL_CONSTRAINT = "uk_project_approval_project_user";

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
            if (!violatesUniqueApproval(exception)) {
                throw exception;
            }
            throw new DuplicateProjectApprovalException(exception);
        }
    }

    private boolean violatesUniqueApproval(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return UNIQUE_APPROVAL_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName());
            }
        }
        return false;
    }
}
