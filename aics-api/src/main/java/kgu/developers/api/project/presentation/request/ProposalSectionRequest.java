package kgu.developers.api.project.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ProposalSectionRequest(
    @Schema(description = "담당 팀원 학번. 담당자를 비우려면 null", example = "202312345")
    @Size(max = 20)
    String assigneeUserId,

    @Schema(description = "작성 완료 여부", example = "true", requiredMode = REQUIRED)
    @NotNull
    Boolean completed
) {
}
