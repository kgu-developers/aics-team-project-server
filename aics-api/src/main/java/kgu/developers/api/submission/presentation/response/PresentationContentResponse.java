package kgu.developers.api.submission.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.presentationcontent.domain.PresentationContent;

@Builder
public record PresentationContentResponse(

        @Schema(description = "팀 소개 텍스트")
        String introText,

        @Schema(description = "주요 기능 목록")
        JsonNode features,

        @Schema(description = "주요 화면 목록")
        JsonNode screens,

        @Schema(description = "시연 영상 유튜브 링크")
        String youtubeUrl
) {

    public static PresentationContentResponse from(PresentationContent content) {
        if (content == null) {
            return PresentationContentResponse.builder().build();
        }
        return PresentationContentResponse.builder()
                .introText(content.getIntroText())
                .features(content.getFeatures())
                .screens(content.getScreens())
                .youtubeUrl(content.getYoutubeUrl())
                .build();
    }
}
