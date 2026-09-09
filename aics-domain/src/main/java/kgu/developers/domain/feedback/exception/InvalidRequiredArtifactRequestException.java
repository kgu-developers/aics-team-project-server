package kgu.developers.domain.feedback.exception;

import static kgu.developers.domain.feedback.exception.FeedbackDomainExceptionCode.INVALID_REQUIRED_ARTIFACT_REQUEST;

import kgu.developers.common.exception.CustomException;

public class InvalidRequiredArtifactRequestException extends CustomException {
    public InvalidRequiredArtifactRequestException(Throwable cause) {
        super(INVALID_REQUIRED_ARTIFACT_REQUEST, cause);
    }
}
