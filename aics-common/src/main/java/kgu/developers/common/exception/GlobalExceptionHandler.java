package kgu.developers.common.exception;

import static kgu.developers.common.exception.GlobalExceptionCode.ACCESS_DENIED;
import static kgu.developers.common.exception.GlobalExceptionCode.DATA_CONFLICT;
import static kgu.developers.common.exception.GlobalExceptionCode.INVALID_INPUT;
import static kgu.developers.common.exception.GlobalExceptionCode.SERVER_ERROR;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// TODO: aics-auth 구현 시 ACCESS_DENIED 코드를 실제 권한 체계(예: 관리자 전용 API)에 맞춰 세분화한다.
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("[{}] {}", ACCESS_DENIED.getCode(), exception.getMessage());
        ErrorResponse response = new ErrorResponse(ACCESS_DENIED.getCode(), exception.getMessage());
        return ResponseEntity.status(FORBIDDEN).body(response);
    }

    // 원본 제약 이름(테이블/컬럼명)이 클라이언트에 노출되지 않도록 고정 메시지만 응답하고,
    // 실제 원인은 서버 로그에만 남긴다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("[{}] {}", DATA_CONFLICT.getCode(), exception.getMostSpecificCause().getMessage());
        ErrorResponse response = new ErrorResponse(DATA_CONFLICT.getCode(), DATA_CONFLICT.getMessage());
        return ResponseEntity.status(DATA_CONFLICT.getStatus()).body(response);
    }

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ExceptionResponse> handleCustomException(CustomException exception) {
        if (exception.isServerError()) {
            eventPublisher.publishEvent(exception);
        }
        ExceptionResponse response = ExceptionResponse.from(exception);
        return ResponseEntity.status(response.status()).body(response);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ExceptionResponse> handleException(Exception exception) {
        eventPublisher.publishEvent(exception);
        return ResponseEntity.internalServerError().body(ExceptionResponse.from(SERVER_ERROR));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ExceptionResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
            .map(violation -> leafNode(violation.getPropertyPath()) + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));
        ExceptionResponse response = ExceptionResponse.of(INVALID_INPUT.getStatus(), INVALID_INPUT.getCode(), message);

        return ResponseEntity.status(response.status()).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
                                                                              HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String message = exception.getParameterValidationResults().stream()
            .map(ParameterValidationResult::getResolvableErrors)
            .flatMap(List::stream)
            .map(MessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));
        ExceptionResponse response = ExceptionResponse.of(INVALID_INPUT.getStatus(), INVALID_INPUT.getCode(), message);

        return ResponseEntity.status(response.status()).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                    HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String message = exception.getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ExceptionResponse response = ExceptionResponse.of(INVALID_INPUT.getStatus(), INVALID_INPUT.getCode(), message);

        return ResponseEntity.status(response.status()).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
                                                          HttpHeaders headers,
                                                          HttpStatusCode status,
                                                          WebRequest request) {
        String message = String.format("Failed to convert '%s' with value: '%s'.",
            exception.getPropertyName(),
            exception.getValue());
        ExceptionResponse response = ExceptionResponse.of(INVALID_INPUT.getStatus(), INVALID_INPUT.getCode(), message);

        return ResponseEntity.status(response.status()).body(response);
    }

    private static String leafNode(Path propertyPath) {
        String path = propertyPath.toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
