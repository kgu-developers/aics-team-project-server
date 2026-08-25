package kgu.developers.api.topicvote.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import kgu.developers.api.topicvote.application.TopicVoteFacade;
import kgu.developers.api.topicvote.presentation.response.TopicVotePersistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TopicVoteControllerImpl implements TopicVoteController {

    private final TopicVoteFacade topicVoteFacade;

    @Override
    @PostMapping("/topic-candidates/{candidateId}/vote")
    public ResponseEntity<TopicVotePersistResponse> vote(@PathVariable Long candidateId, Authentication authentication) {
        return ResponseEntity.status(CREATED).body(topicVoteFacade.vote(candidateId, authentication.getName()));
    }

    @Override
    @DeleteMapping("/topic-candidates/{candidateId}/vote")
    public ResponseEntity<Void> cancelVote(@PathVariable Long candidateId, Authentication authentication) {
        topicVoteFacade.cancelVote(candidateId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
