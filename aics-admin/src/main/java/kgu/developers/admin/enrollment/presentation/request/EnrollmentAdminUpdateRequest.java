package kgu.developers.admin.enrollment.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;

public record EnrollmentAdminUpdateRequest(
    @Schema(description = "역할", example = "ASSISTANT")
    Role role,

    @Schema(description = "상태", example = "WITHDRAWN")
    Status status
) {
}
