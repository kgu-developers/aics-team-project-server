package kgu.developers.domain.feedback.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeedbackDomainExceptionCode implements ExceptionCode {
    REQUIRED_ARTIFACT_NOT_FOUND(NOT_FOUND, "해당 필수 산출물을 찾을 수 없습니다."),
    INVALID_REQUIRED_ARTIFACT_REQUEST(BAD_REQUEST, "필수 산출물 요청 값이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
