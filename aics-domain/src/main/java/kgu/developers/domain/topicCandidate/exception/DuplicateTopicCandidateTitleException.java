package kgu.developers.domain.topicCandidate.exception;

import static kgu.developers.domain.topicCandidate.exception.TopicCandidateDomainExceptionCode.DUPLICATE_TOPIC_CANDIDATE_TITLE;

import kgu.developers.common.exception.CustomException;

public class DuplicateTopicCandidateTitleException extends CustomException {
    public DuplicateTopicCandidateTitleException() {
        super(DUPLICATE_TOPIC_CANDIDATE_TITLE);
    }
}
