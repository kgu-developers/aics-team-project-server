package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PeerEvaluationTeammateAssessmentResponse(
    @Schema(description = "피평가자 학번", example = "20260002", requiredMode = REQUIRED)
    String targetUserId,

    @Schema(description = "피평가자 이름", example = "이서연", requiredMode = REQUIRED)
    String targetUserName,

    @Schema(description = "팀원 기여 내용 상세", example = "프론트엔드 UI 컴포넌트 개발을 주도적으로 진행함")
    String contributionDetail,

    @Schema(description = "팀원 상호 평가 코멘트", example = "협업 시 소통이 원활하고 일정을 잘 준수함")
    String teammateAssessment
) {
}
