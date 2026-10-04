package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_RECORD_NO_UPDATE_FIELD;

import kgu.developers.common.exception.CustomException;

public class MeetingRecordNoUpdateFieldException extends CustomException {

    public MeetingRecordNoUpdateFieldException() {
        super(MEETING_RECORD_NO_UPDATE_FIELD);
    }
}
