package kgu.developers.auth.api.presentation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
@JsonInclude(NON_NULL)
public record MessageResponse(
        @Schema(description = "응답 메시지", requiredMode = REQUIRED)
        String message,

        @Schema(description = "역할 (로그인 응답에만 포함)")
        String role
) {
    public static MessageResponse of(String message) {
        return of(message, null);
    }

    public static MessageResponse of(String message, String role) {
        return MessageResponse.builder()
                .message(message)
                .role(role)
                .build();
    }
}
