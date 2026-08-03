package kgu.developers.api.teammessage.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

// 명시적 상태값을 받는 방식을 채택한다(멱등성 확보를 위해 무조건 토글하는 방식 대신 사용).
@Builder
public record TeamMessageImportantUpdateRequest(

    @Schema(description = "중요 표시 여부", example = "true", requiredMode = REQUIRED)
    @NotNull
    Boolean important
) { }
