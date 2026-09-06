package kgu.developers.domain.topicVote.exception;

import static kgu.developers.domain.topicVote.exception.TopicVoteDomainExceptionCode.TOPIC_VOTE_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class TopicVoteNotFoundException extends CustomException {
    public TopicVoteNotFoundException() {
        super(TOPIC_VOTE_NOT_FOUND);
    }
}
