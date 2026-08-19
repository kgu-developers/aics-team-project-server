package kgu.developers.admin.section.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record SectionAdminPersistResponse(
        @Schema(description = "분반 ID", example = "1", requiredMode = REQUIRED)
        Long id
) {
    public static SectionAdminPersistResponse of(Long id) {
        return new SectionAdminPersistResponse(id);
    }
}
