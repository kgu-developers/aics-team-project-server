package kgu.developers.api.submission.presentation.request;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

public record PresentationContentRequest(
        @Schema(description = "팀 소개 텍스트", example = "저희 팀은 학습관리 플랫폼을 만들었습니다.")
        String introText,

        @Schema(description = "주요 기능 목록 (JSON 배열, 형식 자유: [{title, description}])")
        JsonNode features,

        @Schema(description = "주요 화면 목록 (JSON 배열, 형식 자유: [{imageFileId, caption}])")
        JsonNode screens,

        @Schema(description = "시연 영상 유튜브 링크", example = "https://youtube.com/watch?v=example")
        String youtubeUrl
) {
}
