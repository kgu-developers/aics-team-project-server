package kgu.developers.admin.evaluation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ProfessorPresentationEvaluationAdminResponse(
    Long milestoneId,
    Long teamId,
    @Schema(description = "평가 기간이 현재 열려 있어 수정 가능한지 여부") boolean editable,
    @Schema(description = "최종 저장 시각. 미평가 시 null") LocalDateTime submittedAt,
    List<ProfessorPresentationScoreAdminResponse> scores,
    @Schema(description = "교수자 전용 비공개 메모") String memo
) {
}
