package kgu.developers.common.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalExceptionCode implements ExceptionCode {
    INVALID_INPUT(BAD_REQUEST, "유효한 입력 형식이 아닙니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 파일 크기를 초과했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(FORBIDDEN, "접근 권한이 없습니다."),
    DATA_CONFLICT(CONFLICT, "요청이 기존 데이터와 충돌합니다."),
    SERVER_ERROR(INTERNAL_SERVER_ERROR, "예상치 못한 문제가 발생했습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
