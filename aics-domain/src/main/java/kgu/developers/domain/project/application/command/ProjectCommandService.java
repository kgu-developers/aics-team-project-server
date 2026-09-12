package kgu.developers.domain.project.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionRepository;
import kgu.developers.domain.project.domain.ProposalSectionType;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.exception.ProposalSectionAssigneeNotMemberException;
import kgu.developers.domain.project.exception.ProposalSectionIncompleteException;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectApprovalRepository projectApprovalRepository;
    private final ProposalSectionRepository proposalSectionRepository;
    private final TeamMemberRepository teamMemberRepository;

    public Project saveProject(
        Long teamId, String title, String description, String goal, String repositoryUrl, JsonNode externalLinks,
        JsonNode dataConfiguration, JsonNode screenConfiguration, String projectSchedule
    ) {
        return saveProject(teamId, title, description, goal, repositoryUrl, externalLinks, null, dataConfiguration, screenConfiguration, projectSchedule);
    }

    public Project saveProject(
        Long teamId,
        String title,
        String description,
        String goal,
        String repositoryUrl,
        JsonNode externalLinks,
        Long topicCandidateId,
        JsonNode dataConfiguration,
        JsonNode screenConfiguration,
        String projectSchedule
    ) {
        // 팀당 프로젝트는 하나다. 조회-수정-저장이 갈라지지 않도록 팀 행을 먼저 잠근다.
        projectRepository.lockTeam(teamId);

        Project existing = projectRepository.findIncludingDeletedByTeamId(teamId).orElse(null);
        if (existing == null) {
            return projectRepository.save(Project.create(
                teamId, title, description, goal, repositoryUrl, externalLinks, ApprovalStatus.DRAFT, topicCandidateId, dataConfiguration, screenConfiguration, projectSchedule
            ));
        }
        if (existing.getDeletedAt() != null) {
            return reactivateProject(existing, title, description, goal, repositoryUrl, externalLinks, topicCandidateId, dataConfiguration, screenConfiguration, projectSchedule);
        }
        return updateProject(existing, title, description, goal, repositoryUrl, externalLinks, topicCandidateId, dataConfiguration, screenConfiguration, projectSchedule);
    }

    public Project finalizeTopic(Long teamId, Long topicCandidateId, String title, String description, String goal) {
        projectRepository.lockTeam(teamId);

        Project active = projectRepository.findIncludingDeletedByTeamId(teamId)
            .filter(project -> project.getDeletedAt() == null)
            .orElse(null);

        Project project = saveProject(
            teamId,
            title,
            description,
            goal,
            active == null ? null : active.getRepositoryUrl(),
            active == null ? null : active.getExternalLinks(),
            topicCandidateId,
            // 주제 확정은 제안서 내용을 입력받지 않는다. 기존 제안서가 있으면 그대로 옮기고,
            // 없으면(최초 확정이라 프로젝트를 새로 만드는 경우) NOT NULL이라 빈 배열로 채운다.
            // 배열은 요청마다 새로 만든다 — ArrayNode는 가변이라 상수로 공유하면 안 된다.
            active == null ? JsonNodeFactory.instance.arrayNode() : active.getDataConfiguration(),
            active == null ? JsonNodeFactory.instance.arrayNode() : active.getScreenConfiguration(),
            active == null ? null : active.getProjectSchedule()
        );

        return project;
    }

    /**
     * 소프트 삭제된 프로젝트를 새 제안서로 되살린다.
     * 되살린 제안서는 새 리비전이고, 이전 리비전의 동의는 모두 무효가 된다.
     */
    private Project reactivateProject(
        Project project,
        String title,
        String description,
        String goal,
        String repositoryUrl,
        JsonNode externalLinks,
        Long topicCandidateId,
        JsonNode dataConfiguration,
        JsonNode screenConfiguration,
        String projectSchedule
    ) {
        projectApprovalRepository.deleteAllByProjectId(project.getId());
        proposalSectionRepository.deleteAllByProjectId(project.getId());
        return projectRepository.reactivate(project.getId(), Project.create(
            project.getTeamId(), title, description, goal, repositoryUrl, externalLinks, ApprovalStatus.DRAFT, topicCandidateId, dataConfiguration, screenConfiguration, projectSchedule
        ));
    }

    private Project updateProject(
        Project project,
        String title,
        String description,
        String goal,
        String repositoryUrl,
        JsonNode externalLinks,
        Long topicCandidateId,
        JsonNode dataConfiguration,
        JsonNode screenConfiguration,
        String projectSchedule
    ) {
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }

        if (project.hasSameProposalContent(title, description, goal, repositoryUrl, externalLinks, dataConfiguration, screenConfiguration, projectSchedule) &&
            (topicCandidateId == null || topicCandidateId.equals(project.getTopicCandidateId()))) {
            return project;
        }

        // 변경된 필드에 따라 해당 섹션의 완료 상태를 해제 (업데이트 전에 실행)
        resetCompletedSectionsForChanges(project, title, description, goal, topicCandidateId, dataConfiguration, screenConfiguration, projectSchedule);

        project.updateTitle(title);
        project.updateDescription(description);
        project.updateGoal(goal);
        project.updateDataConfiguration(dataConfiguration);
        project.updateScreenConfiguration(screenConfiguration);
        project.updateProjectSchedule(projectSchedule);
        project.updateRepositoryUrl(repositoryUrl);
        project.updateExternalLinks(externalLinks);
        if (topicCandidateId != null) {
            project.updateTopicCandidateId(topicCandidateId);
        }
        if (project.getApprovalStatus() != ApprovalStatus.REVISION_REQUESTED) {
            project.updateApprovalStatus(ApprovalStatus.DRAFT);
        }
        project.increaseProposalRevision();
        projectApprovalRepository.deleteAllByProjectId(project.getId());

        return projectRepository.save(project);
    }

    private void resetCompletedSectionsForChanges(
        Project project,
        String title,
        String description,
        String goal,
        Long topicCandidateId,
        JsonNode dataConfiguration,
        JsonNode screenConfiguration,
        String projectSchedule
    ) {
        // TOPIC 섹션: title, description, goal, topicCandidateId 변경 시 완료 상태 해제
        if (!Objects.equals(project.getTitle(), title) 
            || !Objects.equals(project.getDescription(), description)
            || !Objects.equals(project.getGoal(), goal)
            || (topicCandidateId != null && !Objects.equals(project.getTopicCandidateId(), topicCandidateId))) {
            proposalSectionRepository.findByProjectIdAndType(project.getId(), ProposalSectionType.TOPIC)
                .ifPresent(section -> {
                    section.forceIncomplete();
                    proposalSectionRepository.save(section);
                });
        }

        // DATA 섹션: dataConfiguration 변경 시 완료 상태 해제
        if (project.isDataConfigurationChanged(dataConfiguration)) {
            proposalSectionRepository.findByProjectIdAndType(project.getId(), ProposalSectionType.DATA)
                .ifPresent(section -> {
                    section.forceIncomplete();
                    proposalSectionRepository.save(section);
                });
        }

        // SCREEN 섹션: screenConfiguration 변경 시 완료 상태 해제
        if (project.isScreenConfigurationChanged(screenConfiguration)) {
            proposalSectionRepository.findByProjectIdAndType(project.getId(), ProposalSectionType.SCREEN)
                .ifPresent(section -> {
                    section.forceIncomplete();
                    proposalSectionRepository.save(section);
                });
        }

        // TEAM_OPERATION 섹션: projectSchedule 변경 시 완료 상태 해제
        if (project.isProjectScheduleChanged(projectSchedule)) {
            proposalSectionRepository.findByProjectIdAndType(project.getId(), ProposalSectionType.TEAM_OPERATION)
                .ifPresent(section -> {
                    section.forceIncomplete();
                    proposalSectionRepository.save(section);
                });
        }
    }

    /**
     * 제안서 5번(팀 운영방식)은 킥오프 정보를 그대로 보여준다. 그래서 킥오프가 바뀌면 제안서 내용이
     * 바뀐 것과 같고, 이전 리비전에 대한 동의는 무효가 된다.
     * 호출부(TeamFacade)가 이미 팀 행을 잠근 뒤에 부른다.
     * ponytail: 이미 제출 완료된 제안서는 건드리지 않는다. 제출 후 킥오프 수정을 막을지는 A파트 정책이라
     * 여기서 정하지 않는다 — 막기로 하면 이 필터를 예외로 바꾼다.
     */
    public void invalidateProposalForKickoffChange(Long teamId) {
        projectRepository.findIncludingDeletedByTeamId(teamId)
            .filter(project -> project.getDeletedAt() == null && project.getProposalCompletedAt() == null)
            .ifPresent(project -> {
                project.increaseProposalRevision();
                projectRepository.save(project);
                projectApprovalRepository.deleteAllByProjectId(project.getId());
                
                // 킥오프 내용 변경 시 TEAM_OPERATION 섹션의 완료 상태 해제
                proposalSectionRepository.findByProjectIdAndType(project.getId(), ProposalSectionType.TEAM_OPERATION)
                    .ifPresent(section -> {
                        section.forceIncomplete();
                        proposalSectionRepository.save(section);
                    });
            });
    }

    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
        projectApprovalRepository.deleteAllByProjectId(projectId);
        proposalSectionRepository.deleteAllByProjectId(projectId);
    }

    public void lockTeam(Long teamId) {
        projectRepository.lockTeam(teamId);
    }

    /**
     * 섹션 담당자·작성 완료 상태를 갱신한다. 없던 섹션은 이 시점에 만든다.
     * 여러 팀원이 같은 섹션을 동시에 갱신해도 행이 두 개 생기지 않도록 팀 행을 먼저 잠근다.
     */
    public ProposalSection updateProposalSection(
        Long projectId,
        ProposalSectionType type,
        String assigneeUserId,
        boolean completed
    ) {
        Long teamId = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new)
            .getTeamId();
        projectRepository.lockTeam(teamId);
        // 잠그기 전에 읽은 스냅숏으로 완료 여부를 판정하면 그사이 커밋된 제안 완료를 놓친다.
        // 잠금 순서는 팀 → 프로젝트로, saveProject·completeProposal 과 같게 유지한다(교착 방지).
        Project project = projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(ProjectNotFoundException::new);

        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        if (assigneeUserId != null
            && teamMemberRepository.findByTeamIdAndUserId(project.getTeamId(), assigneeUserId).isEmpty()) {
            throw new ProposalSectionAssigneeNotMemberException();
        }

        ProposalSection section = proposalSectionRepository.findByProjectIdAndType(projectId, type)
            .orElseGet(() -> ProposalSection.create(projectId, type));
        section.assign(assigneeUserId);
        section.updateCompleted(completed);

        return proposalSectionRepository.save(section);
    }

    /**
     * 팀장이 제안서를 최종 제출한다. 교수님 피드백에 따라 팀원 동의는 선행 조건이 아니고,
     * 고정 섹션이 모두 작성 완료된 것만 확인한다.
     */
    public void completeProposal(Long projectId) {
        Project project = findProjectAfterLockingTeam(projectId);
        if (project.getProposalCompletedAt() != null) {
            throw new ProjectProposalCompletedException();
        }
        Set<ProposalSectionType> completedSections = proposalSectionRepository.findAllByProjectId(projectId).stream()
            .filter(ProposalSection::isCompleted)
            .map(ProposalSection::getType)
            .collect(toSet());
        if (!completedSections.containsAll(EnumSet.allOf(ProposalSectionType.class))) {
            throw new ProposalSectionIncompleteException();
        }
        project.completeProposal();
        projectRepository.save(project);
    }

    public void reopenProposal(Long projectId) {
        Project project = findProjectAfterLockingTeam(projectId);
        if (project.getProposalCompletedAt() == null) {
            return;
        }
        project.reopenProposalForRevision();
        projectApprovalRepository.deleteAllByProjectId(projectId);
        projectRepository.save(project);
    }

    private Project findProjectAfterLockingTeam(Long projectId) {
        Long teamId = projectRepository.findById(projectId)
            .orElseThrow(ProjectNotFoundException::new)
            .getTeamId();
        projectRepository.lockTeam(teamId);
        Project project = projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(ProjectNotFoundException::new);
        if (!teamId.equals(project.getTeamId())) {
            throw new ProjectNotFoundException();
        }
        return project;
    }
}
