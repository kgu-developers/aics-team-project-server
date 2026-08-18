package kgu.developers.domain.teamMember.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamMemberDomainExceptionCode implements ExceptionCode {
    LEADER_ALREADY_EXISTS(CONFLICT, "해당 팀에는 이미 팀장이 있습니다."),
    TEAM_MEMBER_NOT_FOUND(NOT_FOUND, "해당 팀원을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
