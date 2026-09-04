package kgu.developers.domain.fileobject.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.fileobject.exception.FileObjectExceptionCode.FILE_UPLOAD_FAILED;

public class FileUploadFailedException extends CustomException {
    public FileUploadFailedException(Throwable cause) {
        super(FILE_UPLOAD_FAILED, cause);
    }
}
