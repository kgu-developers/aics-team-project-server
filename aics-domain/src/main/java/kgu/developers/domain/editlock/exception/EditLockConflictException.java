package kgu.developers.domain.editlock.exception;

import static kgu.developers.domain.editlock.exception.EditLockExceptionCode.EDIT_LOCK_CONFLICT;

import kgu.developers.common.exception.CustomException;

public class EditLockConflictException extends CustomException {

    public EditLockConflictException() {
        super(EDIT_LOCK_CONFLICT);
    }
}
