package kgu.developers.domain.topicVote.exception;

import static kgu.developers.domain.topicVote.exception.TopicVoteDomainExceptionCode.TOPIC_VOTE_CANDIDATE_CHANGED;

import kgu.developers.common.exception.CustomException;

public class TopicVoteCandidateChangedException extends CustomException {
    public TopicVoteCandidateChangedException() {
        super(TOPIC_VOTE_CANDIDATE_CHANGED);
    }
}
