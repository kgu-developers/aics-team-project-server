package kgu.developers.domain.project.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectDomainExceptionCode implements ExceptionCode {
    PROJECT_NOT_FOUND(NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_VERSION_CONFLICT(CONFLICT, "프로젝트가 다른 사용자에 의해 수정되었습니다. 다시 시도해주세요."),
    PROJECT_ALREADY_EXISTS(CONFLICT, "해당 팀에 이미 프로젝트가 존재합니다."),
    PROJECT_PROPOSAL_COMPLETED(FORBIDDEN, "완료된 프로젝트 제안서는 수정할 수 없습니다."),
    PROJECT_APPROVAL_REQUIRED(PRECONDITION_REQUIRED, "모든 팀원의 프로젝트 제안서 승인이 필요합니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
