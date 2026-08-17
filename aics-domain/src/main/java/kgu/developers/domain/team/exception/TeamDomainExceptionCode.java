package kgu.developers.domain.team.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamDomainExceptionCode implements ExceptionCode {
    TEAM_NOT_FOUND(NOT_FOUND, "해당 팀을 찾을 수 없습니다."),
    TEAM_ALREADY_CONFIRMED(CONFLICT, "확정된 팀은 수정할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
