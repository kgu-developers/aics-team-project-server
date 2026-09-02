package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;

@Builder
public record SubmissionResponse(

        @Schema(description = "제출 식별자", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "팀 식별자", example = "10", requiredMode = REQUIRED)
        Long teamId,

        @Schema(description = "마일스톤 식별자", example = "3", requiredMode = REQUIRED)
        Long milestoneId,

        @Schema(description = "제출 상태", example = "SUBMITTED", requiredMode = REQUIRED)
        SubmissionStatus status,

        @Schema(description = "최신 버전 번호(0이면 아직 제출 안 함)", example = "2", requiredMode = REQUIRED)
        int currentVersion,

        @Schema(description = "지금 제출 가능한지(마감/수정기간/조기오픈 계산 결과)", example = "true", requiredMode = REQUIRED)
        boolean canSubmitNow,

        @Schema(description = "대기 중인 피드백(수정요청)이 있는지", example = "false", requiredMode = REQUIRED)
        boolean hasPendingReview,

        @Schema(description = "발표 순서(발표 마일스톤 전용, 없으면 null)", example = "3")
        Integer presentationOrder
) {

    public static SubmissionResponse of(Submission submission, boolean canSubmitNow, boolean hasPendingReview) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .teamId(submission.getTeamId())
                .milestoneId(submission.getMilestoneId())
                .status(submission.getStatus())
                .currentVersion(submission.getCurrentVersion())
                .canSubmitNow(canSubmitNow)
                .hasPendingReview(hasPendingReview)
                .presentationOrder(submission.getPresentationOrder())
                .build();
    }
}
