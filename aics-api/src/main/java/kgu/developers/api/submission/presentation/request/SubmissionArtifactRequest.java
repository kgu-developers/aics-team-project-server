package kgu.developers.api.submission.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import kgu.developers.domain.submission.domain.ArtifactType;

// LINK/TEXT 아티팩트 전용(파일은 별도 files 파트 + fileArtifactIds로 correlate).
public record SubmissionArtifactRequest(
        @Schema(description = "요구 산출물 식별자", example = "1", requiredMode = REQUIRED)
        @NotNull
        Long requiredArtifactId,

        @Schema(description = "아티팩트 종류(LINK 또는 TEXT)", example = "LINK", requiredMode = REQUIRED)
        @NotNull
        ArtifactType type,

        @Schema(description = "LINK일 때의 URL", example = "https://github.com/kgu-developers/aics-team-project-server")
        String url,

        @Schema(description = "TEXT일 때의 본문", example = "이번 버전에서는 로그인 기능을 완성했습니다.")
        String content
) {
}
