package kgu.developers.api.teammessage.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TeamMessageImportantUpdateRequest(

    @Schema(description = "중요 표시 여부", example = "true", requiredMode = REQUIRED)
    @NotNull
    Boolean important
) { }
