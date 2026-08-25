package kgu.developers.api.topiccandidate.application;

import java.util.List;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
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
}
