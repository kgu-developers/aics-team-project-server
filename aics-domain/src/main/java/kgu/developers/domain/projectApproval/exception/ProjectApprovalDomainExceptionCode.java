package kgu.developers.domain.projectApproval.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectApprovalDomainExceptionCode implements ExceptionCode {
    PROJECT_APPROVAL_NOT_FOUND(NOT_FOUND, "해당 프로젝트에 대한 동의 기록을 찾을 수 없습니다."),
    DUPLICATE_PROJECT_APPROVAL(CONFLICT, "이미 해당 프로젝트에 동의한 사용자입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
