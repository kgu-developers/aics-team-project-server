package kgu.developers.admin.midreport.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kgu.developers.common.response.PageableResponse;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MidReportFeedbackAdminPageResponse(
    @Schema(description = "피드백 목록")
    List<MidReportFeedbackAdminResponse> contents,

    @Schema(description = "페이지 정보")
    PageableResponse<MidReportFeedbackAdminResponse> pageable
) {
    public static MidReportFeedbackAdminPageResponse from(Page<MidReportFeedbackAdminResponse> page) {
        PageableResponse<MidReportFeedbackAdminResponse> pageable = PageableResponse.<MidReportFeedbackAdminResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .isEnd(page.isLast())
            .build();
        return MidReportFeedbackAdminPageResponse.builder()
            .contents(page.getContent())
            .pageable(pageable)
            .build();
    }
}
