package kgu.developers.api.project.presentation.request;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ProjectRequest(
    @Schema(description = "프로젝트 제목", example = "AI 기반 학습 도우미", requiredMode = REQUIRED)
    @NotBlank
    @Size(max = 200)
    String title,
    
    @Schema(description = "프로젝트 설명", example = "개인별 학습 기록을 분석하는 서비스", requiredMode = REQUIRED)
    @NotBlank 
    String description,
    
    @Schema(description = "프로젝트 목표", example = "학습 피드백 자동화", requiredMode = REQUIRED)
    @NotBlank 
    String goal,
    
    @Schema(description = "회의 방식", example = "매주 월요일 대면 회의")
    @Size(max = 200)
    String meetingStyle,

    @Schema(description = "저장소 URL", example = "https://github.com/kgu/project")
    @Size(max = 255)
    String repositoryUrl,
    
    @Schema(description = "외부 링크 목록(JSON)", example = "[{\"name\":\"Figma\",\"url\":\"https://figma.com/...\"}]")
    JsonNode externalLinks
) {
}
