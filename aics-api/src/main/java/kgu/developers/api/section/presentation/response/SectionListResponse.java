package kgu.developers.api.section.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.section.domain.SectionDetail;

public record SectionListResponse(
    @Schema(description = "분반 리스트", requiredMode = REQUIRED)
    List<SectionResponse> contents
) {
    public static SectionListResponse from(List<SectionDetail> details) {
        return new SectionListResponse(details.stream().map(SectionResponse::from).toList());
    }
}
