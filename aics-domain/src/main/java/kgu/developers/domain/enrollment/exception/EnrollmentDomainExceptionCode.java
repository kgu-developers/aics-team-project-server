package kgu.developers.domain.enrollment.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnrollmentDomainExceptionCode implements ExceptionCode {
    ENROLLMENT_NOT_FOUND(NOT_FOUND, "해당 분반에 등록되지 않은 사용자입니다."),
    DUPLICATE_ENROLLMENT(CONFLICT, "이미 해당 분반에 등록된 사용자입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
