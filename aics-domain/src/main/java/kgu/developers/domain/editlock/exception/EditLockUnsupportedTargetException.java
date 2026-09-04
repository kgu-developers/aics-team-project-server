package kgu.developers.domain.editlock.exception;

import static kgu.developers.domain.editlock.exception.EditLockExceptionCode.EDIT_LOCK_UNSUPPORTED_TARGET;

import kgu.developers.common.exception.CustomException;

public class EditLockUnsupportedTargetException extends CustomException {

    public EditLockUnsupportedTargetException() {
        super(EDIT_LOCK_UNSUPPORTED_TARGET);
    }
}
