package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubmissionExceptionCode implements ExceptionCode {

    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 제출을 찾을 수 없습니다."),
    SUBMISSION_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 버전을 찾을 수 없습니다."),
    SUBMISSION_NOT_ALLOWED_NOW(HttpStatus.FORBIDDEN, "지금은 제출할 수 있는 기간이 아닙니다."),
    SUBMISSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "그 팀 소속만 접근할 수 있습니다."),
    SUBMISSION_LEADER_ONLY(HttpStatus.FORBIDDEN, "팀장만 완료 처리할 수 있습니다."),
    SUBMISSION_MEMBER_CONFIRMATION_INCOMPLETE(HttpStatus.PRECONDITION_REQUIRED, "팀원 전원이 아직 확인하지 않았습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
