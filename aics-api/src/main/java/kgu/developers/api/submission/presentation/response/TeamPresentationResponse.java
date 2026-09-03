package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

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

    // content는 호출부(SubmissionFacade)가 imageFileId를 presigned URL로 이미 보강해서 만든
    // 응답을 그대로 받는다 — 원본 도메인 객체를 받아 여기서 다시 변환하면 그 보강을 놓치기 쉽다.
    public static TeamPresentationResponse of(Submission submission, PresentationContentResponse content) {
        return TeamPresentationResponse.builder()
                .teamId(submission.getTeamId())
                .presentationOrder(submission.getPresentationOrder())
                .content(content)
                .build();
    }
}
