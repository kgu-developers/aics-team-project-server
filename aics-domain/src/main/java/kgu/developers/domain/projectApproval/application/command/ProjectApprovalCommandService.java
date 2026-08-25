package kgu.developers.domain.projectApproval.application.command;

import java.time.LocalDateTime;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApprovalCommandService {

    private final ProjectApprovalRepository projectApprovalRepository;

    public void approve(Long projectId, String userId) {
        if (projectApprovalRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new DuplicateProjectApprovalException();
        }
        projectApprovalRepository.save(ProjectApproval.create(projectId, userId, LocalDateTime.now()));
    }
}
