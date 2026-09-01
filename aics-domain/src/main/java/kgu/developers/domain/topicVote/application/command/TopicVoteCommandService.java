package kgu.developers.domain.topicVote.application.command;

import java.util.Optional;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import kgu.developers.domain.topicVote.exception.TopicVoteCandidateChangedException;
import kgu.developers.domain.topicVote.exception.TopicVoteConflictException;
import kgu.developers.domain.topicVote.exception.TopicVoteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        
        try {
            return topicVoteRepository.save(TopicVote.create(teamId, candidateId, voterUserId));
        } catch (DataIntegrityViolationException e) {
            return handleVoteConflict(teamId, candidateId, voterUserId);
        }
    }

    @Transactional
    public TopicVote handleVoteConflict(Long teamId, Long candidateId, String voterUserId) {
        Optional<TopicVote> retryVote = topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(teamId, voterUserId);
        if (retryVote.isPresent()) {
            TopicVote vote = retryVote.get();
            if (vote.getDeletedAt() == null) {
                return changeVoteCandidate(vote, candidateId);
            }
            return reactivateVote(vote, candidateId);
        }
        throw new TopicVoteConflictException();
    }

    @Transactional
    public void cancelVote(Long teamId, Long candidateId, String voterUserId) {
        Optional<TopicVote> vote = topicVoteRepository.findByCandidateIdAndVoterUserIdWithLock(candidateId, voterUserId);
        if (vote.isEmpty()) {
            throw new TopicVoteNotFoundException();
        }
        TopicVote existingVote = vote.get();
        if (!existingVote.getCandidateId().equals(candidateId)) {
            throw new TopicVoteCandidateChangedException();
        }
        existingVote.delete();
        topicVoteRepository.save(existingVote);
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
