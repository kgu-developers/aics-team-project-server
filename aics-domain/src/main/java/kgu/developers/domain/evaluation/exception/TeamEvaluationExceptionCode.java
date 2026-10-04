package kgu.developers.domain.evaluation.exception;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
    TEAM_EVALUATION_CRITERION_NOT_FOUND(NOT_FOUND, "발표 평가 항목을 찾을 수 없습니다."),
    TEAM_EVALUATION_CRITERION_LOCKED(CONFLICT, "평가가 시작됐거나 점수가 저장되어 평가 항목을 변경할 수 없습니다."),
    INVALID_PROFESSOR_PRESENTATION_EVALUATION(BAD_REQUEST, "교수자 발표 평가 점수 또는 메모가 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
