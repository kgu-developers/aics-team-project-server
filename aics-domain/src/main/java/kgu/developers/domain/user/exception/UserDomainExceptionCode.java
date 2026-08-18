package kgu.developers.domain.user.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserDomainExceptionCode implements ExceptionCode {
    USER_NOT_FOUND(NOT_FOUND, "해당 회원을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(UNAUTHORIZED, "학번 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    DUPLICATE_STUDENT_NUMBER(CONFLICT, "이미 존재하는 학번입니다."),
    DUPLICATE_EMAIL(CONFLICT, "이미 사용 중인 이메일입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
