package kgu.developers.domain.project.application.query;

import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projectRepository;

    public Project getProjectByTeamId(Long teamId) {
        return projectRepository.findAllByTeamId(teamId).stream()
            .findFirst()
            .orElseThrow(ProjectNotFoundException::new);
    }
}
