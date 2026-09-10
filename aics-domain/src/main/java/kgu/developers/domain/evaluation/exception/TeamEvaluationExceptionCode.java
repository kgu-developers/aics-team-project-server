package kgu.developers.domain.evaluation.exception;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TeamEvaluationExceptionCode implements ExceptionCode {
    TEAM_EVALUATION_CLOSED(FORBIDDEN, "발표 평가 제출 기간이 아닙니다."),
    INVALID_TEAM_EVALUATION_RESPONSE(UNPROCESSABLE_ENTITY, "발표 평가 응답이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
