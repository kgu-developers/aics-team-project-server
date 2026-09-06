package kgu.developers.domain.topicCandidate.exception;

import static kgu.developers.domain.topicCandidate.exception.TopicCandidateDomainExceptionCode.TOPIC_CANDIDATE_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class TopicCandidateNotFoundException extends CustomException {
    public TopicCandidateNotFoundException() {
        super(TOPIC_CANDIDATE_NOT_FOUND);
    }
}
