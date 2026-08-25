package kgu.developers.api.topiccandidate.presentation;

import kgu.developers.api.topiccandidate.application.TopicCandidateFacade;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TopicCandidateControllerImpl implements TopicCandidateController {

    private final TopicCandidateFacade topicCandidateFacade;

    @Override
    @GetMapping("/teams/{teamId}/topic-candidates")
    public ResponseEntity<TopicCandidateListResponse> getTopicCandidates(
        @PathVariable Long teamId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(topicCandidateFacade.getTopicCandidates(teamId, authentication.getName()));
    }
}
