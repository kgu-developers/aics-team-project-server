package kgu.developers.domain.fileobject.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.fileobject.exception.FileObjectExceptionCode.FILE_OBJECT_NOT_FOUND;

public class FileObjectNotFoundException extends CustomException {
    public FileObjectNotFoundException() {
        super(FILE_OBJECT_NOT_FOUND);
    }
}
