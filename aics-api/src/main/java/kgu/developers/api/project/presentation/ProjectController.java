package kgu.developers.api.project.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Project", description = "프로젝트 제안서 API")
public interface ProjectController {

    @Operation(summary = "프로젝트 제안서 조회")
    ResponseEntity<ProjectResponse> getProject(@PathVariable Long teamId, Authentication authentication);

    @Operation(summary = "프로젝트 제안서 등록 또는 수정")
    ResponseEntity<ProjectResponse> saveProject(
        @PathVariable Long teamId,
        @Valid @RequestBody ProjectRequest request,
        Authentication authentication
    );

    @Operation(summary = "프로젝트 제안 단계 완료")
    ResponseEntity<Void> completeProposal(@PathVariable Long projectId, Authentication authentication);
}
