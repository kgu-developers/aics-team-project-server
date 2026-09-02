package kgu.developers.domain.preSurveyResponse.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PreSurveyResponseDomainExceptionCode implements ExceptionCode {
	PRE_SURVEY_RESPONSE_PREFERRED_ROLES_INVALID(INTERNAL_SERVER_ERROR, "희망 역할 데이터를 변환하지 못했습니다."),
	PRE_SURVEY_RESPONSE_NOT_FOUND(NOT_FOUND, "제출한 사전조사 응답이 없습니다."),
	;

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
