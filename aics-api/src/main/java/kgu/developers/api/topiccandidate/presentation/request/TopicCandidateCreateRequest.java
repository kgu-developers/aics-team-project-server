package kgu.developers.api.topiccandidate.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record TopicCandidateCreateRequest(

    @Schema(description = "주제 제목", example = "AI 기반 학습 도우미", requiredMode = REQUIRED)
    @NotBlank
    @Size(max = 200)
    String title,

    @Schema(description = "주제 설명", example = "학생별 맞춤형 학습 계획을 지원하는 서비스", requiredMode = REQUIRED)
    @NotBlank
    String description
) {
}
