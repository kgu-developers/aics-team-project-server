package kgu.developers.auth.api.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record LoginRequest(
        @Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
        @NotBlank
        String studentNumber,

        @Schema(description = "비밀번호", example = "12345678", requiredMode = REQUIRED)
        @NotBlank
        String password
) {
}
