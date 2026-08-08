package kgu.developers.common.exception;

import java.util.stream.Collectors;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String INVALID_REQUEST = "INVALID_REQUEST";

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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException e) {
		String detail = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		return badRequest(detail);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleInvalidParameter(HandlerMethodValidationException e) {
		String detail = e.getAllErrors().stream()
			.map(MessageSourceResolvable::getDefaultMessage)
			.collect(Collectors.joining(", "));

		return badRequest(detail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
		String detail = e.getConstraintViolations().stream()
			.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
			.collect(Collectors.joining(", "));

		return badRequest(detail);
	}

	private ResponseEntity<ErrorResponse> badRequest(String detail) {
		log.warn("[{}] {}", INVALID_REQUEST, detail);
		return ResponseEntity.badRequest().body(new ErrorResponse(INVALID_REQUEST, detail));
	}
}
