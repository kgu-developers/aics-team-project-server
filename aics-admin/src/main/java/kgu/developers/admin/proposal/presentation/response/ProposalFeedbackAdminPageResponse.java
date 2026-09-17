package kgu.developers.admin.proposal.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kgu.developers.common.response.PageableResponse;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record ProposalFeedbackAdminPageResponse(
    @Schema(description = "피드백 목록")
    List<ProposalFeedbackAdminResponse> contents,

    @Schema(description = "페이지 정보")
    PageableResponse<ProposalFeedbackAdminResponse> pageable
) {
    public static ProposalFeedbackAdminPageResponse from(Page<ProposalFeedbackAdminResponse> page) {
        PageableResponse<ProposalFeedbackAdminResponse> pageable = PageableResponse.<ProposalFeedbackAdminResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .isEnd(page.isLast())
            .build();
        return ProposalFeedbackAdminPageResponse.builder()
            .contents(page.getContent())
            .pageable(pageable)
            .build();
    }
}
