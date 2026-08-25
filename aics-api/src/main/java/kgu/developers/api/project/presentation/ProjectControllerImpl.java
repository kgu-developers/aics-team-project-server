package kgu.developers.api.project.presentation;

import jakarta.validation.Valid;
import kgu.developers.api.project.application.ProjectFacade;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectControllerImpl implements ProjectController {

    private final ProjectFacade projectFacade;

    @Override
    @GetMapping("/teams/{teamId}/project")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long teamId, Authentication authentication) {
        return ResponseEntity.ok(projectFacade.getProject(teamId, authentication.getName()));
    }

    @Override
    @PutMapping("/teams/{teamId}/project")
    public ResponseEntity<ProjectResponse> saveProject(
        @PathVariable Long teamId,
        @Valid @RequestBody ProjectRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(projectFacade.saveProject(teamId, authentication.getName(), request));
    }
}
