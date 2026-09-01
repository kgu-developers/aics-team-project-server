package kgu.developers.domain.meetingrecord.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_ACTION_NOT_FOUND;

public class MeetingActionNotFoundException extends CustomException {

    public MeetingActionNotFoundException() {
        super(MEETING_ACTION_NOT_FOUND);
    }
}
