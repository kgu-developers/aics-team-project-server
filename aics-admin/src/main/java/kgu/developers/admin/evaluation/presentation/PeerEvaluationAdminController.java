package kgu.developers.admin.evaluation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Peer Evaluation Admin", description = "관리자 상호평가 결과 조회 API")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "관리자 권한 또는 담당 분반 접근 권한 없음"),
    @ApiResponse(responseCode = "404", description = "분반, 팀 또는 상호평가 양식을 찾을 수 없음")
})
public interface PeerEvaluationAdminController {

    @Operation(summary = "분반별 상호평가 현황 목록 조회", description = "분반 내 팀별 상호평가 제출 현황 및 요약을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PeerEvaluationAdminListResponse.class)))
    ResponseEntity<PeerEvaluationAdminListResponse> getPeerEvaluations(
        @Parameter(description = "분반 ID", example = "1", required = true)
        @Positive @PathVariable Long sectionId,
        @Parameter(description = "상호평가 양식 ID (생략 시 최신 양식 조회)", example = "10")
        @RequestParam(required = false) Long formId,
        @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "팀 상호평가 결과 상세 조회", description = "특정 팀의 팀원 간 상호평가 기여도 매트릭스, 서술형 답변 및 회의록 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = PeerEvaluationAdminTeamDetailResponse.class)))
    ResponseEntity<PeerEvaluationAdminTeamDetailResponse> getTeamPeerEvaluationDetail(
        @Parameter(description = "분반 ID", example = "1", required = true)
        @Positive @PathVariable Long sectionId,
        @Parameter(description = "팀 ID", example = "1", required = true)
        @Positive @PathVariable Long teamId,
        @Parameter(description = "상호평가 양식 ID (생략 시 최신 양식 조회)", example = "10")
        @RequestParam(required = false) Long formId,
        @Parameter(hidden = true) Authentication authentication);
}
