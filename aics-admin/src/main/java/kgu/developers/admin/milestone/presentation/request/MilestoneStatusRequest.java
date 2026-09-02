package kgu.developers.admin.milestone.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kgu.developers.domain.milestone.domain.MilestoneStatus;

public record MilestoneStatusRequest(
        @Schema(description = "공개 상태", example = "PUBLISHED", requiredMode = REQUIRED)
        @NotNull
        MilestoneStatus status
) {
}
