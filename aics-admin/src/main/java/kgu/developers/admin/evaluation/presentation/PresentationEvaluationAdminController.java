package kgu.developers.admin.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Presentation Evaluation Admin", description = "관리자 발표 평가 결과 조회 API")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "관리자 권한 또는 담당 분반 접근 권한 없음"),
    @ApiResponse(responseCode = "404", description = "분반, 마일스톤 또는 팀을 찾을 수 없음")
})
public interface PresentationEvaluationAdminController {

    @Operation(summary = "분반별 발표 평가 현황 목록 조회", description = "분반 내 팀별 발표 평가 항목별 평균 점수 및 합계 요약을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PresentationEvaluationAdminListResponse.class)))
    ResponseEntity<PresentationEvaluationAdminListResponse> getPresentationEvaluations(
        @Parameter(description = "분반 ID", example = "1", required = true)
        @Positive @PathVariable Long sectionId,
        @Parameter(description = "마일스톤 ID (생략 시 분반의 발표 마일스톤 자동 조회)", example = "3")
        @RequestParam(required = false) Long milestoneId,
        @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "팀 발표 평가 결과 상세 조회", description = "특정 팀에 대한 학생(평가자)별 발표 평가 항목별 점수, 합계 및 팀 회의록 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PresentationEvaluationAdminTeamDetailResponse.class)))
    ResponseEntity<PresentationEvaluationAdminTeamDetailResponse> getTeamPresentationEvaluationDetail(
        @Parameter(description = "분반 ID", example = "1", required = true)
        @Positive @PathVariable Long sectionId,
        @Parameter(description = "팀 ID", example = "1", required = true)
        @Positive @PathVariable Long teamId,
        @Parameter(description = "마일스톤 ID (생략 시 분반의 발표 마일스톤 자동 조회)", example = "3")
        @RequestParam(required = false) Long milestoneId,
        @Parameter(hidden = true) Authentication authentication);
}
