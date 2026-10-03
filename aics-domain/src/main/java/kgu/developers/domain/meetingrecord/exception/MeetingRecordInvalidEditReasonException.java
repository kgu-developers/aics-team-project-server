package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_RECORD_INVALID_EDIT_REASON;

import kgu.developers.common.exception.CustomException;

public class MeetingRecordInvalidEditReasonException extends CustomException {

    public MeetingRecordInvalidEditReasonException() {
        super(MEETING_RECORD_INVALID_EDIT_REASON);
    }
}
