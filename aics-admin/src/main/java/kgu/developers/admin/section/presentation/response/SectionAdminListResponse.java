package kgu.developers.admin.section.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.section.domain.SectionDetail;
import lombok.Builder;

@Builder
public record SectionAdminListResponse(
    @Schema(description = "분반 리스트", requiredMode = REQUIRED)
    List<SectionAdminResponse> contents
) {
    public static SectionAdminListResponse from(List<SectionDetail> details) {
        return SectionAdminListResponse.builder()
                .contents(details.stream().map(SectionAdminResponse::from).toList())
                .build();
    }
}
