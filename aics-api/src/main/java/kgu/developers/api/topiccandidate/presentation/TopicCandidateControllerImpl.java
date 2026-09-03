package kgu.developers.api.topiccandidate.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import kgu.developers.api.topiccandidate.application.TopicCandidateFacade;
import kgu.developers.api.topiccandidate.presentation.request.TopicCandidateCreateRequest;
import kgu.developers.api.topiccandidate.presentation.request.TopicFinalizeRequest;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidatePersistResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicFinalizeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
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

    @Override
    @PostMapping("/teams/{teamId}/topic-candidates")
    public ResponseEntity<TopicCandidatePersistResponse> createTopicCandidate(
        @PathVariable Long teamId,
        @Valid @RequestBody TopicCandidateCreateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(CREATED)
            .body(topicCandidateFacade.createTopicCandidate(teamId, authentication.getName(), request));
    }

    @Override
    @PatchMapping("/teams/{teamId}/topic-finalize")
    public ResponseEntity<TopicFinalizeResponse> finalizeTopic(
        @PathVariable Long teamId,
        @Valid @RequestBody TopicFinalizeRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(topicCandidateFacade.finalizeTopic(teamId, authentication.getName(), request));
    }
}
