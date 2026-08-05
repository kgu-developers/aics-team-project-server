package kgu.developers.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
		ExceptionCode code = e.getCode();

		if (e.isServerError()) {
			log.error("[{}] {}", code.getCode(), code.getMessage(), e);
		} else {
			log.warn("[{}] {}", code.getCode(), code.getMessage());
		}

		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.from(code));
	}
}
