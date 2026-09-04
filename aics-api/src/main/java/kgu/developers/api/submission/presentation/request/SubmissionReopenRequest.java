package kgu.developers.api.submission.presentation.request;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record SubmissionReopenRequest(
        @Schema(description = "이 시각까지 재제출을 허용한다(미래 시각만 가능)", example = "2026-09-10T23:59:00", requiredMode = REQUIRED)
        @NotNull
        @Future
        LocalDateTime revisionDueAt
) {
}
