package kgu.developers.domain.topicVote.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicVoteCommandService {
  private final TopicVoteRepository topicVoteRepository;
  private final TopicCandidateRepository topicCandidateRepository;

  public TopicVote vote(Long teamId, Long candidateId, String voterUserId) {
    TopicCandidate candidate = topicCandidateRepository.findById(candidateId)
            .orElseThrow(TopicCandidateNotFoundException::new);
    if (!candidate.getTeamId().equals(teamId)) {
      throw new TopicCandidateNotFoundException();
    }
    return topicVoteRepository.upsert(TopicVote.create(teamId, candidateId, voterUserId));
  }

  public void cancelVote(Long teamId, Long candidateId, String voterUserId) {
    topicVoteRepository.deleteByTeamIdAndCandidateIdAndVoterUserId(teamId, candidateId, voterUserId);
  }
}
