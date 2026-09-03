package kgu.developers.admin.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Team Evaluation Criterion", description = "팀 발표 평가 항목 API")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "관리자 권한 또는 담당 분반 접근 권한 없음")
})
public interface TeamEvaluationCriterionController {

  @Operation(summary = "팀 발표 평가 항목 목록 조회", description = "분반의 평가 항목을 표시 순서대로 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      content = @Content(schema = @Schema(implementation = TeamEvaluationCriterionListResponse.class)))
  ResponseEntity<TeamEvaluationCriterionListResponse> getCriteria(
      @Parameter(description = "분반 ID", example = "1", required = true)
      @Positive @PathVariable Long sectionId,
      @Parameter(hidden = true) Authentication authentication);

  @Operation(summary = "팀 발표 평가 항목 생성", description = "분반에서 사용할 팀 발표 평가 항목을 생성합니다.")
  @ApiResponse(
      responseCode = "201",
      content = @Content(schema = @Schema(implementation = TeamEvaluationCriterionPersistResponse.class)))
  ResponseEntity<TeamEvaluationCriterionPersistResponse> createCriterion(
      @Parameter(description = "분반 ID", example = "1", required = true)
      @Positive @PathVariable Long sectionId,
      @Valid @RequestBody TeamEvaluationCriterionCreateRequest request,
      @Parameter(hidden = true) Authentication authentication);
}
