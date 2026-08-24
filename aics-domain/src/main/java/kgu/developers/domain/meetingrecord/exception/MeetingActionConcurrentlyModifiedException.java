package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_ACTION_CONCURRENTLY_MODIFIED;

import kgu.developers.common.exception.CustomException;

public class MeetingActionConcurrentlyModifiedException extends CustomException {

    public MeetingActionConcurrentlyModifiedException() {
        super(MEETING_ACTION_CONCURRENTLY_MODIFIED);
    }
}
