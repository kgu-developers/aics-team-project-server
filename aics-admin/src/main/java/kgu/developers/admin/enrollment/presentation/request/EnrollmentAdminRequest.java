package kgu.developers.admin.enrollment.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kgu.developers.domain.enrollment.domain.Role;

public record EnrollmentAdminRequest(
    @Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
    @NotBlank
    String studentNumber,

    @Schema(description = "역할", example = "STUDENT", requiredMode = REQUIRED)
    @NotNull
    Role role
) {
}
