package kgu.developers.domain.section.exception;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SectionDomainExceptionCode implements ExceptionCode {
    SECTION_NOT_FOUND(NOT_FOUND, "해당 분반을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
