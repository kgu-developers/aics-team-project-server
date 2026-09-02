package kgu.developers.api.submission.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresentationOrderRequest(
        @Schema(description = "팀별 발표 순서 목록(분반의 모든 팀을 중복 없이 포함해야 한다)", requiredMode = REQUIRED)
        @NotEmpty
        List<TeamOrder> teamOrders
) {
    public record TeamOrder(
            @NotNull Long teamId,
            @NotNull @Positive Integer order
    ) {
    }
}
