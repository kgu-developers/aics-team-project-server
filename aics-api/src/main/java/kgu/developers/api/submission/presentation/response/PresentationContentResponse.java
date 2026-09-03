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

    // screens는 호출부(SubmissionFacade)가 imageFileId를 presigned URL로 보강해 넘겨준 걸 그대로
    // 쓴다 — 원본 content.getScreens()를 여기서 직접 읽지 않는 이유는, 그러면 이 메서드를 새로
    // 쓰는 곳마다 보강을 깜빡하고 imageFileId만 내려주는 실수가 반복될 수 있기 때문이다.
    public static PresentationContentResponse from(PresentationContent content, JsonNode resolvedScreens) {
        if (content == null) {
            return PresentationContentResponse.builder().build();
        }
        return PresentationContentResponse.builder()
                .introText(content.getIntroText())
                .features(content.getFeatures())
                .screens(resolvedScreens)
                .youtubeUrl(content.getYoutubeUrl())
                .build();
    }
}
