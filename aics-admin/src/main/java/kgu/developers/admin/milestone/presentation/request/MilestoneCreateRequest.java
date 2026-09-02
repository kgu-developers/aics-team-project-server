package kgu.developers.admin.milestone.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kgu.developers.domain.milestone.domain.MilestoneType;

public record MilestoneCreateRequest(
        @Schema(description = "마일스톤 제목", example = "프로젝트 제안서", requiredMode = REQUIRED)
        @NotBlank
        @Size(max = 100)
        String title,

        @Schema(description = "마일스톤 설명", example = "팀 프로젝트 제안서를 제출합니다.")
        String description,

        @Schema(description = "진행 주차", example = "2", requiredMode = REQUIRED)
        @Positive
        int weekNumber,

        @Schema(description = "마일스톤 일정", requiredMode = REQUIRED)
        @Valid
        @NotNull
        MilestoneScheduleRequest schedule,

        @Schema(description = "마일스톤 유형(안 보내면 GENERAL). 최종보고서 완료게이트 등 B3 로직이 이 값으로 동작한다.",
                example = "GENERAL")
        MilestoneType type
) {
}
