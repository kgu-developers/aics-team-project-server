package kgu.developers.domain.teamMember.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
    TEAM_MEMBER_ALREADY_EXISTS(CONFLICT, "이미 팀에 등록된 팀원입니다."),
    TEAM_MEMBER_NOT_FOUND(NOT_FOUND, "해당 팀원을 찾을 수 없습니다."),
    TEAM_MEMBER_CONCURRENTLY_MODIFIED(CONFLICT, "다른 요청이 먼저 팀원 정보를 변경했습니다. 다시 시도해주세요."),
    TEAM_MEMBER_SECTION_MISMATCH(BAD_REQUEST, "같은 분반의 팀으로만 옮길 수 있습니다."),
    LEADER_MOVE_REQUIRES_EXPLICIT_ROLE(CONFLICT,
        "팀장을 다른 팀으로 옮기려면 isLeader 를 함께 보내 팀장 유지 여부를 지정해야 합니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
