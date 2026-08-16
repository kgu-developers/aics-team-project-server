package kgu.developers.common.exception;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String INVALID_REQUEST = "INVALID_REQUEST";
	private static final String DATA_CONFLICT = "DATA_CONFLICT";
	private static final String ACCESS_DENIED = "ACCESS_DENIED";

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
			.map(violation -> leafNode(violation.getPropertyPath()) + ": " + violation.getMessage())
			.collect(Collectors.joining(", "));

		return badRequest(detail);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
		log.warn("[{}] {}", DATA_CONFLICT, e.getMostSpecificCause().getMessage());
		return ResponseEntity.status(CONFLICT)
			.body(new ErrorResponse(DATA_CONFLICT, "요청이 기존 데이터와 충돌합니다."));
	}

	// 컨트롤러에서 던진 AccessDeniedException은 여기서 잡지 않으면 스프링 시큐리티의 기본
	// 처리로 넘어가 /error의 기본 바디로 나간다. 공통 {code, message} 형식을 유지한다.
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
		log.warn("[{}] {}", ACCESS_DENIED, e.getMessage());
		return ResponseEntity.status(FORBIDDEN)
			.body(new ErrorResponse(ACCESS_DENIED, e.getMessage()));
	}

	private static String leafNode(Path propertyPath) {
		String path = propertyPath.toString();
		return path.substring(path.lastIndexOf('.') + 1);
	}

	private ResponseEntity<ErrorResponse> badRequest(String detail) {
		log.warn("[{}] {}", INVALID_REQUEST, detail);
		return ResponseEntity.badRequest().body(new ErrorResponse(INVALID_REQUEST, detail));
	}
}
