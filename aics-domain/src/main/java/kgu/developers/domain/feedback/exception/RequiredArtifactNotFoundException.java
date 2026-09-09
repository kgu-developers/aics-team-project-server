package kgu.developers.domain.feedback.exception;

import static kgu.developers.domain.feedback.exception.FeedbackDomainExceptionCode.REQUIRED_ARTIFACT_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class RequiredArtifactNotFoundException extends CustomException {
    public RequiredArtifactNotFoundException() {
        super(REQUIRED_ARTIFACT_NOT_FOUND);
    }
}
