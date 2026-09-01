package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_ACTION_INVALID_CONTENT;

import kgu.developers.common.exception.CustomException;

public class MeetingActionInvalidContentException extends CustomException {

    public MeetingActionInvalidContentException() {
        super(MEETING_ACTION_INVALID_CONTENT);
    }
}
