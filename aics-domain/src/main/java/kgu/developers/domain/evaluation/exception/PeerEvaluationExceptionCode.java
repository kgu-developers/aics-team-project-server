package kgu.developers.domain.evaluation.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PeerEvaluationExceptionCode implements ExceptionCode {
    PEER_EVALUATION_FORM_NOT_FOUND(NOT_FOUND, "상호평가 양식을 찾을 수 없습니다."),
    PEER_EVALUATION_CLOSED(FORBIDDEN, "상호평가 제출 기간이 아닙니다."),
    PEER_EVALUATION_ALREADY_SUBMITTED(CONFLICT, "이미 제출한 상호평가는 수정할 수 없습니다."),
    INVALID_PEER_EVALUATION_RESPONSE(UNPROCESSABLE_ENTITY, "상호평가 응답이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
