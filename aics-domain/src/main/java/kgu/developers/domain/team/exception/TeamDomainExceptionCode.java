package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.domain.Team.MIN_UNFINALIZE_REASON_LENGTH;
import static kgu.developers.domain.team.domain.Team.MAX_UNFINALIZE_REASON_LENGTH;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamDomainExceptionCode implements ExceptionCode {
    TEAM_UNFINALIZE_COMPLETED_FINAL_REPORT(CONFLICT, "완료된 최종보고서를 먼저 재개한 후 팀 확정을 취소해 주세요."),
    TEAM_INVALID_UNFINALIZE_REASON(BAD_REQUEST, "팀 확정 취소 사유는 " + MIN_UNFINALIZE_REASON_LENGTH
        + "자 이상 " + MAX_UNFINALIZE_REASON_LENGTH + "자 이하여야 합니다."),
    TEAM_NOT_FOUND(NOT_FOUND, "해당 팀을 찾을 수 없습니다."),
    TEAM_ALREADY_CONFIRMED(CONFLICT, "확정된 팀은 수정할 수 없습니다."),
    TEAM_CONCURRENTLY_MODIFIED(CONFLICT, "다른 요청이 팀을 먼저 수정했습니다. 다시 시도해 주세요."),
    DUPLICATE_TEAM_NAME(CONFLICT, "같은 분반에 이미 있는 팀명입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
