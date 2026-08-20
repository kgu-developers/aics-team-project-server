package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_RECORD_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class MeetingRecordNotFoundException extends CustomException {

    public MeetingRecordNotFoundException() {
        super(MEETING_RECORD_NOT_FOUND);
    }
}
