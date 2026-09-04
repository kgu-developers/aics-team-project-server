package kgu.developers.domain.topicVote.application.command;

import java.util.Optional;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import kgu.developers.domain.topicVote.exception.TopicVoteCandidateChangedException;
import kgu.developers.domain.topicVote.exception.TopicVoteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TopicVoteCommandService {

    private final TopicVoteRepository topicVoteRepository;
    private final TopicCandidateRepository topicCandidateRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public TopicVote vote(Long teamId, Long candidateId, String voterUserId) {
        teamRepository.findByIdForUpdate(teamId).orElseThrow(TeamNotFoundException::new);
        validateActiveCandidate(teamId, candidateId);

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

    // 파사드의 후보 조회와 투표 저장 사이에 후보가 삭제될 수 있으므로, 투표 트랜잭션
    // 안에서 팀 행을 잠근 채 다시 확인한다. 후보의 teamId 는 #62 이후 불변이라,
    // 팀이 다르면 다른 팀 기준으로 소속이 검증된 요청이다.
    private void validateActiveCandidate(Long teamId, Long candidateId) {
        TopicCandidate candidate = topicCandidateRepository.findByIdForUpdate(candidateId)
            .orElseThrow(TopicCandidateNotFoundException::new);
        if (!candidate.getTeamId().equals(teamId)) {
            throw new TopicCandidateNotFoundException();
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
