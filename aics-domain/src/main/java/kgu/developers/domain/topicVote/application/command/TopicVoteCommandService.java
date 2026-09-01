package kgu.developers.domain.topicVote.application.command;

import java.util.Optional;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
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
        throw new IllegalStateException("동시 투표 충돌로 인해 기존 투표를 찾을 수 없습니다");
    }

    @Transactional
    public void cancelVote(Long teamId, Long candidateId, String voterUserId) {
        Optional<TopicVote> vote = topicVoteRepository.findByCandidateIdAndVoterUserIdWithLock(candidateId, voterUserId);
        if (vote.isPresent()) {
            TopicVote existingVote = vote.get();
            if (!existingVote.getCandidateId().equals(candidateId)) {
                throw new IllegalStateException("투표 후보가 변경되어 취소할 수 없습니다");
            }
            existingVote.delete();
            topicVoteRepository.save(existingVote);
        }
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
