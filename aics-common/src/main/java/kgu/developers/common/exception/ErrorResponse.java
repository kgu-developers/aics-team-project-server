package kgu.developers.common.exception;

public record ErrorResponse(
    String code
) {
    public static ErrorResponse from(ExceptionCode code) {
        return new ErrorResponse(code.getCode());
    }
}
