package kgu.developers.api.editlock.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import lombok.Builder;

@Builder
public record EditLockAcquireRequest(

    @Schema(description = "잠금 대상 종류", example = "PRESENTATION_CONTENT", requiredMode = REQUIRED)
    @NotNull
    EditLockTargetType targetType,

    @Schema(description = "잠금 대상 id", example = "1", requiredMode = REQUIRED)
    @NotNull
    @Positive
    Long targetId,

    @Schema(description = "대상 안에서 잠글 섹션 구분자(섹션이 없는 대상은 고정값 하나를 정해 사용)",
        example = "TEAM_INFO", requiredMode = REQUIRED)
    @NotBlank
    String sectionKey
) {
}
