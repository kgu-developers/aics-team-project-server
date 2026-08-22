package kgu.developers.domain.milestone.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MilestoneDomainExceptionCode implements ExceptionCode {
    MILESTONE_NOT_FOUND(NOT_FOUND, "해당 마일스톤을 찾을 수 없습니다."),
    MILESTONE_SECTION_FORBIDDEN(FORBIDDEN, "요청한 분반의 마일스톤이 아닙니다."),
    MILESTONE_WEEK_CONFLICT(CONFLICT, "같은 분반에서 마일스톤 주차를 중복할 수 없습니다."),
    INVALID_MILESTONE_REQUEST(BAD_REQUEST, "마일스톤 요청 값이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
