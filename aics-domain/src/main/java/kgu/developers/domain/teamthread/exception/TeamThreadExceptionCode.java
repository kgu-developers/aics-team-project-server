package kgu.developers.domain.teamthread.exception;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TeamThreadExceptionCode implements ExceptionCode {

    TEAM_THREAD_NOT_FOUND(NOT_FOUND, "해당 팀의 커뮤니케이션 스레드를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
