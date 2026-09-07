package kgu.developers.domain.preSurveyResponse.exception;

import static kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseDomainExceptionCode.PRE_SURVEY_RESPONSE_PREFERRED_PEER_INVALID;

import kgu.developers.common.exception.CustomException;

public class PreSurveyResponsePreferredPeerInvalidException extends CustomException {
	public PreSurveyResponsePreferredPeerInvalidException() {
		super(PRE_SURVEY_RESPONSE_PREFERRED_PEER_INVALID);
	}
}
