package kgu.developers.common.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {

    HttpStatus getStatus();

    String getMessage();

    String getCode();
}
