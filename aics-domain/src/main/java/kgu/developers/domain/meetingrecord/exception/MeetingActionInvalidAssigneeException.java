package kgu.developers.domain.meetingrecord.exception;

import static kgu.developers.domain.meetingrecord.exception.MeetingRecordExceptionCode.MEETING_ACTION_INVALID_ASSIGNEE;

import kgu.developers.common.exception.CustomException;

public class MeetingActionInvalidAssigneeException extends CustomException {

    public MeetingActionInvalidAssigneeException() {
        super(MEETING_ACTION_INVALID_ASSIGNEE);
    }
}
