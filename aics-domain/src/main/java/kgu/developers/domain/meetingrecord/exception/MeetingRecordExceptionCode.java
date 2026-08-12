package kgu.developers.domain.meetingrecord.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingRecordExceptionCode implements ExceptionCode {

    MEETING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의록을 찾을 수 없습니다."),
    MEETING_RECORD_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "회의 내용은 공백일 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
