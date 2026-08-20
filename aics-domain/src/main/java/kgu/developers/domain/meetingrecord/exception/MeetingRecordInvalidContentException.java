package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_RECORD_INVALID_CONTENT;

import kgu.developers.common.exception.CustomException;

public class MeetingRecordInvalidContentException extends CustomException {

    public MeetingRecordInvalidContentException() {
        super(MEETING_RECORD_INVALID_CONTENT);
    }
}
