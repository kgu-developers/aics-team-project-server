package kgu.developers.domain.topicVote.application.command;

import java.util.Optional;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TopicVoteCommandService {

    private final TopicVoteRepository topicVoteRepository;

    @Transactional
    public TopicVote vote(Long teamId, Long candidateId, String voterUserId) {
        Optional<TopicVote> existingVote = topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(teamId, voterUserId);
        if (existingVote.isPresent()) {
            TopicVote vote = existingVote.get();
            if (vote.getDeletedAt() == null) {
                return changeVoteCandidate(vote, candidateId);
            }
            return reactivateVote(vote, candidateId);
        }
        
        return topicVoteRepository.save(TopicVote.create(teamId, candidateId, voterUserId));
    }

    public void cancelVote(Long candidateId, String voterUserId) {
        topicVoteRepository.deleteByCandidateIdAndVoterUserId(candidateId, voterUserId);
    }

    private TopicVote changeVoteCandidate(TopicVote existingVote, Long candidateId) {
        if (existingVote.getCandidateId().equals(candidateId)) {
            return existingVote;
        }
        existingVote.updateCandidateId(candidateId);
        return topicVoteRepository.save(existingVote);
    }

    private TopicVote reactivateVote(TopicVote deletedVote, Long candidateId) {
        deletedVote.updateCandidateId(candidateId);
        deletedVote.setDeletedAt(null);
        return topicVoteRepository.save(deletedVote);
    }
}
