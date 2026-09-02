package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.format.DateTimeFormatter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;

@Builder
public record SubmissionMemberConfirmationResponse(

        @Schema(description = "확인한 학번", example = "202412345", requiredMode = REQUIRED)
        String userId,

        @Schema(description = "최종보고서 내용 확인 여부", example = "true", requiredMode = REQUIRED)
        boolean confirmedFinalReport,

        @Schema(description = "아티팩트 확인 여부", example = "true", requiredMode = REQUIRED)
        boolean confirmedArtifacts,

        @Schema(description = "한 줄 소감", example = "고생 많았습니다.")
        String oneLineReview,

        @Schema(description = "확인 일시", example = "2026-09-02 14:00", requiredMode = REQUIRED)
        String confirmedAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static SubmissionMemberConfirmationResponse from(SubmissionMemberConfirmation confirmation) {
        return SubmissionMemberConfirmationResponse.builder()
                .userId(confirmation.getUserId())
                .confirmedFinalReport(confirmation.isConfirmedFinalReport())
                .confirmedArtifacts(confirmation.isConfirmedArtifacts())
                .oneLineReview(confirmation.getOneLineReview())
                .confirmedAt(confirmation.getConfirmedAt().format(FORMATTER))
                .build();
    }
}
