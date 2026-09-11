package kgu.developers.api.project.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectImageUploadResponse(
    @Schema(description = "제안서 화면 구성의 imageFileId에 저장할 파일 식별자", example = "42")
    Long fileId
) {
}
