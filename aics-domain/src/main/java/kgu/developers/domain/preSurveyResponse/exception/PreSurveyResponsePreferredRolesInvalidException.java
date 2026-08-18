package kgu.developers.domain.preSurveyResponse.exception;

import static kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseDomainExceptionCode.PRE_SURVEY_RESPONSE_PREFERRED_ROLES_INVALID;

import kgu.developers.common.exception.CustomException;

public class PreSurveyResponsePreferredRolesInvalidException extends CustomException {
	public PreSurveyResponsePreferredRolesInvalidException(Throwable cause) {
		super(PRE_SURVEY_RESPONSE_PREFERRED_ROLES_INVALID, cause);
	}
}
