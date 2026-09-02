package kgu.developers.domain.preSurveyResponse.exception;

import static kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseDomainExceptionCode.PRE_SURVEY_RESPONSE_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class PreSurveyResponseNotFoundException extends CustomException {
	public PreSurveyResponseNotFoundException() {
		super(PRE_SURVEY_RESPONSE_NOT_FOUND);
	}
}
