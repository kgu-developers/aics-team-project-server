package kgu.developers.api.editlock.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.project.domain.ProposalSectionType;
import lombok.Builder;

@Builder
public record EditLockAcquireRequest(

    @Schema(description = "잠금 대상 종류", example = "PROJECT", requiredMode = REQUIRED)
    @NotNull
    EditLockTargetType targetType,

    @Schema(description = "잠금 대상 id", example = "1", requiredMode = REQUIRED)
    @NotNull
    @Positive
    Long targetId,

    @Schema(description = "대상 안에서 잠글 섹션 구분자. PROJECT는 TOPIC·DATA·SCREEN·TEAM_OPERATION 중 하나이고, "
        + "섹션이 없는 대상은 고정값 하나를 정해 사용한다.",
        example = "SCREEN", requiredMode = REQUIRED)
    @NotBlank
    String sectionKey
) {
    // 제안서 저장은 섹션 이름과 같은 키의 락만 인정한다. 다른 키는 잡혀도 저장이 항상 409가 되므로 여기서 막는다.
    @JsonIgnore
    @AssertTrue(message = "제안서 잠금의 sectionKey는 제안서 섹션 이름이어야 합니다.")
    public boolean isProjectSectionKeyValid() {
        return targetType != EditLockTargetType.PROJECT
            || Arrays.stream(ProposalSectionType.values()).anyMatch(type -> type.name().equals(sectionKey));
    }
}
