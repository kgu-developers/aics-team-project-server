package kgu.developers.domain.section.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SectionDomainExceptionCode implements ExceptionCode {
    SECTION_NOT_FOUND(NOT_FOUND, "해당 분반을 찾을 수 없습니다."),
    INVALID_CAPACITY(BAD_REQUEST, "정원은 0 이상이어야 합니다."),
    INVALID_CONTACT_VISIBLE_PERIOD(BAD_REQUEST, "연락처 공개 종료 시각은 시작 시각보다 빠를 수 없습니다."),
    PROFESSOR_ROLE_REQUIRED(BAD_REQUEST, "분반 담당 교수는 ADMIN 역할의 사용자여야 합니다."),
    CONTACT_NOT_VISIBLE(FORBIDDEN, "지금은 연락처를 공개하지 않는 기간입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
