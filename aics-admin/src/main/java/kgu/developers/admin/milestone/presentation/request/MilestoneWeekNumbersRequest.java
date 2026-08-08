package kgu.developers.admin.milestone.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kgu.developers.domain.milestone.application.command.MilestoneWeekNumberChange;

public record MilestoneWeekNumbersRequest(
        @Schema(description = "마일스톤별 변경 주차", requiredMode = REQUIRED)
        @Valid
        @NotEmpty
        List<MilestoneWeekNumberItem> changes
) {
    public List<MilestoneWeekNumberChange> toDomain() {
        return changes.stream()
                .map(change -> new MilestoneWeekNumberChange(change.milestoneId(), change.weekNumber()))
                .toList();
    }

    public record MilestoneWeekNumberItem(
            @Schema(description = "마일스톤 ID", example = "1", requiredMode = REQUIRED)
            @NotNull
            @Positive
            Long milestoneId,

            @Schema(description = "변경할 주차", example = "3", requiredMode = REQUIRED)
            @Positive
            int weekNumber
    ) {
    }
}
