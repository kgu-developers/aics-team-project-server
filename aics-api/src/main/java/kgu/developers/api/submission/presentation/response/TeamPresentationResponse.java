package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.presentationcontent.domain.PresentationContent;
import kgu.developers.domain.submission.domain.Submission;

@Builder
public record TeamPresentationResponse(

        @Schema(description = "팀 식별자", example = "10", requiredMode = REQUIRED)
        Long teamId,

        @Schema(description = "발표 순서(미지정이면 null)", example = "1")
        Integer presentationOrder,

        @Schema(description = "발표 공개자료", requiredMode = REQUIRED)
        PresentationContentResponse content
) {

    public static TeamPresentationResponse of(Submission submission, PresentationContent content) {
        return TeamPresentationResponse.builder()
                .teamId(submission.getTeamId())
                .presentationOrder(submission.getPresentationOrder())
                .content(PresentationContentResponse.from(content))
                .build();
    }
}
