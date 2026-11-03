package kgu.developers.domain.teammessage.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TeamMessageExceptionCode implements ExceptionCode {

    TEAM_MESSAGE_NOT_FOUND(NOT_FOUND, "해당 메시지를 찾을 수 없습니다."),
    TEAM_MESSAGE_NOT_GROUPED(BAD_REQUEST, "일괄발송으로 묶인 메시지가 아닙니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
