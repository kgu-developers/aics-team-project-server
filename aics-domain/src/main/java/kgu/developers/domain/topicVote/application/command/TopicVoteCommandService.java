package kgu.developers.domain.topicVote.application.command;

import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicVoteCommandService {

    private final TopicVoteRepository topicVoteRepository;

    public TopicVote vote(Long teamId, Long candidateId, String voterUserId) {
        return topicVoteRepository.findByTeamIdAndVoterUserId(teamId, voterUserId)
            .map(existingVote -> changeVoteCandidate(existingVote, candidateId))
            .orElseGet(() -> topicVoteRepository.save(TopicVote.create(teamId, candidateId, voterUserId)));
    }

    private TopicVote changeVoteCandidate(TopicVote existingVote, Long candidateId) {
        if (existingVote.getCandidateId().equals(candidateId)) {
            return existingVote;
        }
        existingVote.updateCandidateId(candidateId);
        return topicVoteRepository.save(existingVote);
    }
}
