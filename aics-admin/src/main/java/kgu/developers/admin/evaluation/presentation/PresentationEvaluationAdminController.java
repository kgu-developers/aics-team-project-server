package kgu.developers.admin.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationEvaluationRequest;
import kgu.developers.admin.evaluation.presentation.response.ProfessorPresentationEvaluationResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "교수자 발표 평가 조회", description = "담당 교수자만 점수와 비공개 메모를 조회합니다. 종료 후에도 조회할 수 있습니다.")
    ResponseEntity<ProfessorPresentationEvaluationResponse> getProfessorEvaluation(
        @Positive @PathVariable Long sectionId,
        @Positive @PathVariable Long milestoneId,
        @Positive @PathVariable Long teamId,
        @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "교수자 발표 평가 저장", description = "평가 기간 중 활성 항목 전체의 점수와 비공개 메모를 저장·수정합니다. 학생 평균에는 포함하지 않습니다.")
    ResponseEntity<ProfessorPresentationEvaluationResponse> saveProfessorEvaluation(
        @Positive @PathVariable Long sectionId,
        @Positive @PathVariable Long milestoneId,
        @Positive @PathVariable Long teamId,
        @Valid @RequestBody ProfessorPresentationEvaluationRequest request,
        @Parameter(hidden = true) Authentication authentication);
}
