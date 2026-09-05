package kgu.developers.domain.topicCandidate.exception;

import static kgu.developers.domain.topicCandidate.exception.TopicCandidateDomainExceptionCode.DUPLICATE_TOPIC_CANDIDATE;

import kgu.developers.common.exception.CustomException;

public class DuplicateTopicCandidateException extends CustomException {
    public DuplicateTopicCandidateException() {
        super(DUPLICATE_TOPIC_CANDIDATE);
    }
}
