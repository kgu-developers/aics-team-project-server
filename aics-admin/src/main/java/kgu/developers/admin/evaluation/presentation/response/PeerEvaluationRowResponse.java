package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import lombok.Builder;

@Builder
public record PeerEvaluationRowResponse(
    @Schema(description = "평가자 학번", example = "20260001", requiredMode = REQUIRED)
    String evaluatorId,

    @Schema(description = "평가자 이름", example = "김민준", requiredMode = REQUIRED)
    String evaluatorName,

    @Schema(description = "팀장 여부", example = "true", requiredMode = REQUIRED)
    boolean isLeader,

    @Schema(description = "제출 상태 (SUBMITTED, DRAFT, 또는 제출 시작 전이면 null)", example = "SUBMITTED")
    PeerEvaluationSubmissionStatus status,

    @Schema(description = "제출 일시 (미제출 시 null)", example = "2026-12-14T15:30:00")
    LocalDateTime submittedAt,

    @Schema(description = "해당 평가자가 다른 팀원들에게 부여한 기여도 평균 점수 (제출 완료 시에만 계산, 미제출 시 null)", example = "30.0")
    Double averageScore,

    @Schema(description = "피평가자별 점수 목록 (팀원 순서대로 정렬)", requiredMode = REQUIRED)
    List<PeerEvaluationScoreDetailResponse> scores,

    @Schema(description = "본인 기여도 및 수행 내용", example = "백엔드 API 설계 및 DB 연동")
    String selfContribution,

    @Schema(description = "프로젝트 전체 총평", example = "프로젝트 완성도가 높았습니다.")
    String projectReviewComment,

    @Schema(description = "개인 회고", example = "많은 것을 배웠습니다.")
    String reflectionComment,

    @Schema(description = "팀원별 상세 기여도 및 평가 코멘트", requiredMode = REQUIRED)
    List<PeerEvaluationTeammateAssessmentResponse> teammateAssessments
) {
}
