package kgu.developers.api.milestone.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;

public record RequiredArtifactResponse(
        @Schema(description = "필수 산출물 식별자. 제출 API의 requiredArtifactId로 사용", example = "1")
        Long id,
        @Schema(allowableValues = {"FILE", "LINK", "TEXT", "CHEERPJ_RUN"})
        RequiredArtifactType type,
        String label,
        boolean required,
        @Schema(description = "FILE 유형의 허용 확장자 목록", example = "[\"pdf\", \"zip\"]")
        List<String> allowedExtensions,
        @Schema(description = "FILE 유형의 최대 파일 크기(MB)", example = "20")
        Integer maxFileSizeMb
) {
    public static RequiredArtifactResponse from(RequiredArtifact requiredArtifact) {
        return new RequiredArtifactResponse(
                requiredArtifact.getId(),
                requiredArtifact.getType(),
                requiredArtifact.getLabel(),
                requiredArtifact.isRequired(),
                parseAllowedExtensions(requiredArtifact.getAllowedExtensions()),
                requiredArtifact.getMaxFileSizeMb()
        );
    }

    private static List<String> parseAllowedExtensions(String allowedExtensions) {
        if (allowedExtensions == null || allowedExtensions.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .filter(extension -> !extension.isEmpty())
                .toList();
    }
}
