package kgu.developers.domain.topicVote.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicVoteCommandService {
  private final TopicVoteRepository topicVoteRepository;

  public TopicVote vote(Long teamId, Long candidateId, String voterUserId) {
    return topicVoteRepository.upsert(TopicVote.create(teamId, candidateId, voterUserId));
  }

  public void cancelVote(Long teamId, String voterUserId) {
    topicVoteRepository.deleteByTeamIdAndVoterUserId(teamId, voterUserId);
  }
}
