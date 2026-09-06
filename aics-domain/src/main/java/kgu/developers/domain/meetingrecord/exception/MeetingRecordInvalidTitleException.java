package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_RECORD_INVALID_TITLE;

import kgu.developers.common.exception.CustomException;

public class MeetingRecordInvalidTitleException extends CustomException {

    public MeetingRecordInvalidTitleException() {
        super(MEETING_RECORD_INVALID_TITLE);
    }
}
