package kgu.developers.domain.projectApproval.application.command;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
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
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /**
     * 현재 제안서 리비전에 대한 동의를 남긴다.
     * 같은 리비전에 무효화된 이력이 있으면 새로 넣지 않고 그 행을 되살린다.
     */
    public Long approve(Long projectId, String userId, LocalDateTime approvedAt) {
        Project project = projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(ProjectNotFoundException::new);
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        if (userRepository.findByStudentNumber(userId).isEmpty()) {
            throw new UserNotFoundException();
        }

        long proposalRevision = project.getProposalRevision();
        ProjectApproval existing = projectApprovalRepository
            .findIncludingDeleted(projectId, userId, proposalRevision)
            .orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new DuplicateProjectApprovalException();
            }
            existing.reactivate(approvedAt);
            return projectApprovalRepository.save(existing).getId();
        }

        // 조회 후 저장은 check-then-act 라 동시 요청을 막지 못한다.
        // 중복 차단의 최종 근거는 uk_project_approval_project_user 제약이고,
        // 위반은 ProjectApprovalRepositoryImpl.save 가 DuplicateProjectApprovalException 으로 바꿔 던진다.
        ProjectApproval projectApproval = ProjectApproval.create(projectId, userId, proposalRevision, approvedAt);
        return projectApprovalRepository.save(projectApproval).getId();
    }
}
