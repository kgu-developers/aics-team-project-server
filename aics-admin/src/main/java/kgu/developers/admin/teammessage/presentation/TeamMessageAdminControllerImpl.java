package kgu.developers.admin.teammessage.presentation;

import kgu.developers.admin.teammessage.application.TeamMessageAdminFacade;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/messages")
public class TeamMessageAdminControllerImpl implements TeamMessageAdminController {

    private final TeamMessageAdminFacade teamMessageAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<TeamMessageAdminPageResponse> getMessages(
        @RequestParam(required = false) Long sectionId,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            teamMessageAdminFacade.getMessages(sectionId, pageable, authentication.getName()));
    }
}
