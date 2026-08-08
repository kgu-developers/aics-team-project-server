package kgu.developers.admin.milestone.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.milestone.domain.Milestone;

public record MilestoneListResponse(
        @Schema(description = "주차순 마일스톤 목록", requiredMode = REQUIRED)
        List<MilestoneResponse> contents
) {
    public static MilestoneListResponse from(List<Milestone> milestones) {
        return new MilestoneListResponse(milestones.stream().map(MilestoneResponse::from).toList());
    }
}
