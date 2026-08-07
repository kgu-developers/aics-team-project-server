package kgu.developers.api.user.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record UserUpdateRequest (
    @Schema(description = "비밀번호", example = "12345678", requiredMode = REQUIRED)
    @NotBlank
    @Size(min = 8, max = 64)
    String password
) {

}
