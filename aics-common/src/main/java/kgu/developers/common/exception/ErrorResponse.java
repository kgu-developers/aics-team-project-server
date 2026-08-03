package kgu.developers.common.exception;

import lombok.Builder;

@Builder
public record ErrorResponse(
    String code
) {
    public static ErrorResponse of(ExceptionCode exceptionCode) {
        return ErrorResponse.builder()
            .code(exceptionCode.getCode())
            .build();
    }

    public static ErrorResponse of(String code) {
        return ErrorResponse.builder()
            .code(code)
            .build();
    }
}
