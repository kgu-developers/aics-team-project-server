package kgu.developers.common.exception;

import static kgu.developers.common.exception.GlobalExceptionCode.ACCESS_DENIED;
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
    public ResponseEntity<ExceptionResponse> handleAccessDeniedException() {
        ExceptionResponse response = ExceptionResponse.from(ACCESS_DENIED);
        return ResponseEntity.status(FORBIDDEN).body(response);
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
