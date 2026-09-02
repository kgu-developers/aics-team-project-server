package kgu.developers.auth.api.presentation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.auth.domain.LoginRole;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
@JsonInclude(NON_NULL)
public record MessageResponse(
        @Schema(description = "응답 메시지", requiredMode = REQUIRED)
        String message,

        @Schema(description = "역할 (로그인/리프레시 응답에만 포함)", allowableValues = {"ADMIN", "STUDENT", "ASSISTANT"})
        LoginRole role
) {
    public static MessageResponse of(String message) {
        return of(message, null);
    }

    public static MessageResponse of(String message, LoginRole role) {
        return MessageResponse.builder()
                .message(message)
                .role(role)
                .build();
    }
}
