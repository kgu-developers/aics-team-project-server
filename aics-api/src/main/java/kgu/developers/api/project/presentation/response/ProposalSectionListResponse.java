package kgu.developers.api.project.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

public record ProposalSectionListResponse(
    @Schema(description = "고정 섹션 구성 전체(양식 순서)", requiredMode = REQUIRED)
    List<ProposalSectionResponse> contents,

    @Schema(description = "모든 섹션 작성 완료 여부(팀장 제출 가능 여부)", requiredMode = REQUIRED)
    boolean allCompleted
) {
    public static ProposalSectionListResponse from(List<ProposalSectionResponse> contents) {
        return new ProposalSectionListResponse(contents, contents.stream().allMatch(ProposalSectionResponse::completed));
    }
}
