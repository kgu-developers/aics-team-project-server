package kgu.developers.api.topicvote.application;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topicvote.presentation.response.TopicVotePersistResponse;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import kgu.developers.domain.topicVote.application.command.TopicVoteCommandService;
import kgu.developers.domain.topicVote.domain.TopicVote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class TopicVoteFacade {

    private final TopicCandidateRepository topicCandidateRepository;
    private final TopicVoteCommandService topicVoteCommandService;
    private final TeamAccessValidator teamAccessValidator;

    public TopicVotePersistResponse vote(Long candidateId, String voterUserId) {
        TopicCandidate topicCandidate = topicCandidateRepository.findById(candidateId)
            .orElseThrow(TopicCandidateNotFoundException::new);
        teamAccessValidator.validateMembership(topicCandidate.getTeamId(), voterUserId);

        TopicVote topicVote = topicVoteCommandService.vote(topicCandidate.getTeamId(), candidateId, voterUserId);
        return TopicVotePersistResponse.of(topicVote);
    }
}
