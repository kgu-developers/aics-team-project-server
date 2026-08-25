package kgu.developers.api.topiccandidate.application;

import java.util.List;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topiccandidate.presentation.request.TopicCandidateCreateRequest;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidatePersistResponse;
import kgu.developers.domain.topicCandidate.application.command.TopicCandidateCommandService;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TopicCandidateFacade {

    private final TopicCandidateCommandService topicCandidateCommandService;
    private final TopicCandidateRepository topicCandidateRepository;
    private final TopicVoteRepository topicVoteRepository;
    private final TeamAccessValidator teamAccessValidator;

    public TopicCandidateListResponse getTopicCandidates(Long teamId, String userId) {
        teamAccessValidator.validateMembership(teamId, userId);

        List<TopicCandidate> candidates = topicCandidateRepository.findByTeamId(teamId);
        List<TopicVote> votes = topicVoteRepository.findAllByCandidateIdIn(
            candidates.stream().map(TopicCandidate::getId).toList()
        );
        return TopicCandidateListResponse.of(candidates, votes, userId);
    }

    @Transactional
    public TopicCandidatePersistResponse createTopicCandidate(
        Long teamId,
        String proposerUserId,
        TopicCandidateCreateRequest request
    ) {
        teamAccessValidator.validateMembership(teamId, proposerUserId);
        TopicCandidate topicCandidate = topicCandidateCommandService.createTopicCandidate(
            teamId,
            proposerUserId,
            request.title(),
            request.description()
        );
        return TopicCandidatePersistResponse.of(topicCandidate);
    }
}
