package kgu.developers.domain.fileobject.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.fileobject.exception.FileObjectExceptionCode.FILE_OBJECT_INVALID_TYPE;

public class FileObjectInvalidTypeException extends CustomException {
    public FileObjectInvalidTypeException() {
        super(FILE_OBJECT_INVALID_TYPE);
    }
}
