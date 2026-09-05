package kgu.developers.domain.meetingrecord.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingRecordExceptionCode implements ExceptionCode {

    MEETING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회의록을 찾을 수 없습니다."),
    MEETING_RECORD_INVALID_TITLE(HttpStatus.BAD_REQUEST, "회의록 제목은 공백일 수 없습니다."),
    MEETING_RECORD_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "회의 내용은 공백일 수 없습니다."),
    MEETING_ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 액션플랜을 찾을 수 없습니다."),
    MEETING_ACTION_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "작업 내용은 공백일 수 없습니다."),
    MEETING_ACTION_INVALID_ASSIGNEE(HttpStatus.BAD_REQUEST, "담당자는 같은 팀의 팀원이어야 합니다."),
    MEETING_ACTION_CONCURRENTLY_MODIFIED(HttpStatus.CONFLICT, "다른 요청이 먼저 액션플랜을 변경했습니다. 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
