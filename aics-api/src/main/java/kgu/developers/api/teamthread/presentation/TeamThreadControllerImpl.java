package kgu.developers.api.teamthread.presentation;

import kgu.developers.api.teamthread.application.TeamThreadFacade;
import kgu.developers.api.teamthread.presentation.response.TeamThreadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TeamThreadControllerImpl implements TeamThreadController {

    private final TeamThreadFacade teamThreadFacade;

    @Override
    @GetMapping("/teams/{teamId}/thread")
    public ResponseEntity<TeamThreadResponse> getThread(@PathVariable Long teamId, Authentication authentication) {
        return ResponseEntity.ok(teamThreadFacade.getOrCreateThread(teamId, authentication.getName()));
    }
}
