package kgu.developers.admin.enrollment.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record EnrollmentAdminPersistResponse(
    @Schema(description = "수강 ID", example = "1", requiredMode = REQUIRED)
    Long id
) {
    public static EnrollmentAdminPersistResponse of(Long id) {
        return new EnrollmentAdminPersistResponse(id);
    }
}
