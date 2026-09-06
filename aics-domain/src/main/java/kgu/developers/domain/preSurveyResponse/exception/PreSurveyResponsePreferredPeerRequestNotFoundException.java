package kgu.developers.domain.preSurveyResponse.exception;

import static kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseDomainExceptionCode.PRE_SURVEY_RESPONSE_PREFERRED_PEER_REQUEST_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class PreSurveyResponsePreferredPeerRequestNotFoundException extends CustomException {
	public PreSurveyResponsePreferredPeerRequestNotFoundException() {
		super(PRE_SURVEY_RESPONSE_PREFERRED_PEER_REQUEST_NOT_FOUND);
	}
}
