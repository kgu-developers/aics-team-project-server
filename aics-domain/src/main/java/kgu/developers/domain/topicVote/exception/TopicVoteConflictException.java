package kgu.developers.domain.topicVote.exception;

import static kgu.developers.domain.topicVote.exception.TopicVoteDomainExceptionCode.TOPIC_VOTE_CONFLICT;

import kgu.developers.common.exception.CustomException;

public class TopicVoteConflictException extends CustomException {
    public TopicVoteConflictException() {
        super(TOPIC_VOTE_CONFLICT);
    }
}
