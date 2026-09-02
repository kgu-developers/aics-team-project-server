package kgu.developers.domain.fileobject.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileObjectExceptionCode implements ExceptionCode {

    FILE_OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
