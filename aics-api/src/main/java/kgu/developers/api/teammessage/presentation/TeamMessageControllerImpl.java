package kgu.developers.api.teammessage.presentation;

import kgu.developers.api.teammessage.application.TeamMessageFacade;
import kgu.developers.api.teammessage.presentation.request.TeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.request.TeamMessageImportantUpdateRequest;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePageResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.UnreadMessageCountResponse;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TeamMessageControllerImpl implements TeamMessageController {

    private final TeamMessageFacade teamMessageFacade;

    @Override
    @GetMapping("/teams/{teamId}/messages")
    public ResponseEntity<TeamMessagePageResponse> getMessages(
        @PathVariable Long teamId,
        @RequestParam(required = false) TeamMessageRelatedType relatedType,
        Pageable pageable
    ) {
        return ResponseEntity.ok(teamMessageFacade.getMessages(teamId, relatedType, pageable));
    }

    @Override
    @PostMapping("/teams/{teamId}/messages")
    public ResponseEntity<TeamMessagePersistResponse> postMessage(
        @PathVariable Long teamId,
        @RequestBody TeamMessageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamMessageFacade.postMessage(teamId, request));
    }

    @Override
    @GetMapping("/teams/{teamId}/unread-message-count")
    public ResponseEntity<UnreadMessageCountResponse> getUnreadCount(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamMessageFacade.getUnreadCount(teamId));
    }

    @Override
    @PatchMapping("/messages/{id}/important")
    public ResponseEntity<Void> updateImportant(
        @PathVariable Long id,
        @RequestBody TeamMessageImportantUpdateRequest request
    ) {
        teamMessageFacade.updateImportant(id, request.important());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        teamMessageFacade.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
