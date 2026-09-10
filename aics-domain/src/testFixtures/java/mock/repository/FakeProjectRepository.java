package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;

public class FakeProjectRepository implements ProjectRepository {

    private final Map<Long, Project> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Project save(Project project) {
        Long id = project.getId() != null ? project.getId() : sequence.incrementAndGet();

        Project saved = Project.builder()
                .id(id)
                .teamId(project.getTeamId())
                .topicCandidateId(project.getTopicCandidateId())
                .title(project.getTitle())
                .description(project.getDescription())
                .goal(project.getGoal())
                .dataConfiguration(project.getDataConfiguration())
                .screenConfiguration(project.getScreenConfiguration())
                .projectSchedule(project.getProjectSchedule())
                .repositoryUrl(project.getRepositoryUrl())
                .externalLinks(project.getExternalLinks())
                .approvalStatus(project.getApprovalStatus())
                .proposalCompletedAt(project.getProposalCompletedAt())
                .proposalRevision(project.getProposalRevision())
                .version(project.getVersion())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .deletedAt(project.getDeletedAt())
                .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Project reactivate(Long projectId, Project newProject) {
        return save(newProject);
    }

    @Override
    public void lockTeam(Long teamId) {
        // No-op for fake
    }

    @Override
    public Optional<Project> findById(Long id) {
        Project project = store.get(id);
        if (project != null && project.getDeletedAt() == null) {
            return Optional.of(project);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Project> findByIdForUpdate(Long id) {
        return findById(id);
    }

    @Override
    public List<Project> findAllByTeamId(Long teamId) {
        return store.values().stream()
                .filter(p -> p.getDeletedAt() == null && teamId.equals(p.getTeamId()))
                .toList();
    }

    @Override
    public List<Project> findAllByTeamIdIn(List<Long> teamIds) {
        return store.values().stream()
                .filter(p -> p.getDeletedAt() == null && teamIds.contains(p.getTeamId()))
                .toList();
    }

    @Override
    public Optional<Project> findIncludingDeletedByTeamId(Long teamId) {
        return store.values().stream()
                .filter(p -> teamId.equals(p.getTeamId()))
                .findFirst();
    }

    @Override
    public void deleteById(Long id) {
        Project project = store.get(id);
        if (project != null) {
            project.delete();
            store.put(id, project);
        }
    }
}
