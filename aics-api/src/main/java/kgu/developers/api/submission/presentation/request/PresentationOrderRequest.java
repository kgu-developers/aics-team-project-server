package kgu.developers.api.submission.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PresentationOrderRequest(
        @Schema(description = "팀별 발표 순서 목록", requiredMode = REQUIRED)
        @NotEmpty
        List<TeamOrder> teamOrders
) {
    public record TeamOrder(
            @NotNull Long teamId,
            @NotNull Integer order
    ) {
    }
}
