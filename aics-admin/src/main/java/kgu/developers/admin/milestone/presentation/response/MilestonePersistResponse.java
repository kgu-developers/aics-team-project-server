package kgu.developers.admin.milestone.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record MilestonePersistResponse(
        @Schema(description = "생성된 마일스톤 ID", example = "1", requiredMode = REQUIRED)
        Long id
) {
    public static MilestonePersistResponse of(Long id) {
        return new MilestonePersistResponse(id);
    }
}
