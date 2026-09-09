package kgu.developers.admin.milestone.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;

public record RequiredArtifactRequest(
        @NotNull
        @Schema(description = "산출물 유형", allowableValues = {"FILE", "LINK", "TEXT", "CHEERPJ_RUN"})
        RequiredArtifactType type,

        @NotBlank @Size(max = 100)
        @Schema(description = "화면에 표시할 산출물 이름", example = "중간보고서 PDF")
        String label,

        @NotNull
        @Schema(description = "제출 시 필수 여부", example = "true")
        Boolean required,

        @Size(max = 20)
        @Schema(description = "FILE 유형의 허용 확장자 목록", example = "[\"pdf\", \"zip\"]")
        List<@NotBlank @Size(max = 20) String> allowedExtensions,

        @Positive
        @Schema(description = "FILE 유형의 최대 파일 크기(MB)", example = "20")
        Integer maxFileSizeMb
) {
    public String allowedExtensionsValue() {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return null;
        }
        List<String> normalized = allowedExtensions.stream()
                .map(String::trim)
                .map(extension -> extension.replaceFirst("^\\.+", "").toLowerCase(Locale.ROOT))
                .filter(extension -> !extension.isBlank())
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }
}
