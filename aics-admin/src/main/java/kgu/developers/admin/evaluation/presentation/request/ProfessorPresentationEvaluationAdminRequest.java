package kgu.developers.admin.evaluation.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfessorPresentationEvaluationAdminRequest(
    @Schema(description = "활성 평가 항목 전체의 점수")
    @NotEmpty List<@Valid ProfessorPresentationScoreAdminRequest> scores,
    @Schema(description = "학생에게 공개되지 않는 교수자 메모", example = "발표 흐름이 명확함")
    @Size(max = 5000) String memo
) {
}
