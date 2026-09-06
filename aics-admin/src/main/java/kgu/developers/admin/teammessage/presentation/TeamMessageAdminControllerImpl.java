package kgu.developers.admin.teammessage.presentation;

import kgu.developers.admin.teammessage.application.TeamMessageAdminFacade;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/messages")
public class TeamMessageAdminControllerImpl implements TeamMessageAdminController {

    private final TeamMessageAdminFacade teamMessageAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<TeamMessageAdminPageResponse> getMessages(
        @RequestParam(required = false) Long sectionId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            teamMessageAdminFacade.getMessages(
                sectionId, PageRequest.of(page, size), authentication.getName()));
    }

    @Override
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable Long id,
        Authentication authentication
    ) {
        teamMessageAdminFacade.markAsRead(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
