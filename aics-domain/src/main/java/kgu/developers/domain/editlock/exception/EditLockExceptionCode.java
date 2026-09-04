package kgu.developers.domain.editlock.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EditLockExceptionCode implements ExceptionCode {

    EDIT_LOCK_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 편집 중입니다."),
    EDIT_LOCK_UNSUPPORTED_TARGET(HttpStatus.NOT_IMPLEMENTED, "아직 지원하지 않는 잠금 대상입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
