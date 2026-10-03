package kgu.developers.admin.sectionartifact.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;

@Builder
public record SectionArtifactSummaryAdminListResponse(
        @Schema(description = "분반 식별자", example = "10", requiredMode = REQUIRED)
        Long sectionId,

        @Schema(description = "분반 표시 문자열(과목번호)", example = "OOP-01", requiredMode = REQUIRED)
        String sectionName,

        @Schema(description = "집계 기준일", example = "2026-11-20", requiredMode = REQUIRED)
        LocalDate asOf,

        @Schema(description = "팀별 산출물 요약 목록", requiredMode = REQUIRED)
        List<SectionArtifactSummaryAdminResponse> contents
) {

    public static SectionArtifactSummaryAdminListResponse of(
            Long sectionId,
            String sectionName,
            LocalDate asOf,
            List<SectionArtifactTeamRow> rows
    ) {
        return SectionArtifactSummaryAdminListResponse.builder()
                .sectionId(sectionId)
                .sectionName(sectionName)
                .asOf(asOf)
                .contents(rows.stream()
                        .map(SectionArtifactSummaryAdminResponse::from)
                        .toList())
                .build();
    }
}
