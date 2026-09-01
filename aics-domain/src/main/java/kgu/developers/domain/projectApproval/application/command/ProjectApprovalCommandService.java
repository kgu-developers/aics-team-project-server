package kgu.developers.domain.projectApproval.application.command;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApprovalCommandService {
    private final ProjectApprovalRepository projectApprovalRepository;
    private final UserRepository userRepository;

    public Long createProjectApproval(Long projectId, String userId, LocalDateTime approvedAt) {
        if (userRepository.findByStudentNumber(userId).isEmpty()) {
            throw new UserNotFoundException();
        }

        ProjectApproval existing = projectApprovalRepository.findIncludingDeleted(projectId, userId).orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new DuplicateProjectApprovalException();
            }
            existing.reactivate(approvedAt);
            return projectApprovalRepository.save(existing).getId();
        }

        ProjectApproval projectApproval = ProjectApproval.create(projectId, userId, approvedAt);
        return projectApprovalRepository.save(projectApproval).getId();
    }
}
