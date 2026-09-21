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
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormUpdateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Peer Evaluation Form", description = "상호평가 양식 API")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 또는 담당 분반 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "분반, 마일스톤 또는 상호평가 양식을 찾을 수 없음")
})
public interface PeerEvaluationFormController {

    @Operation(summary = "상호평가 양식 생성", description = "분반의 마일스톤에 사용할 상호평가 양식을 생성합니다.")
    @ApiResponse(
            responseCode = "201",
            content = @Content(schema = @Schema(implementation = PeerEvaluationFormPersistResponse.class)))
    ResponseEntity<PeerEvaluationFormPersistResponse> createForm(
            @Parameter(description = "분반 ID", example = "1", required = true)
            @Positive @PathVariable Long sectionId,
            @Valid @RequestBody PeerEvaluationFormCreateRequest request,
            @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "상호평가 양식 ID로 조회", description = "양식 ID로 상호평가 양식을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = PeerEvaluationFormResponse.class)))
    ResponseEntity<PeerEvaluationFormResponse> getForm(
            @Parameter(description = "분반 ID", example = "1", required = true)
            @Positive @PathVariable Long sectionId,
            @Parameter(description = "양식 ID", example = "1", required = true)
            @Positive @PathVariable Long formId,
            @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "마일스톤 ID로 상호평가 양식 조회", description = "마일스톤 ID로 연결된 상호평가 양식을 조회합니다.")
    @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = PeerEvaluationFormResponse.class)))
    ResponseEntity<PeerEvaluationFormResponse> getFormByMilestoneId(
            @Parameter(description = "분반 ID", example = "1", required = true)
            @Positive @PathVariable Long sectionId,
            @Parameter(description = "마일스톤 ID", example = "1", required = true)
            @Positive @PathVariable Long milestoneId,
            @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "상호평가 양식 수정", description = "상호평가 양식의 익명 여부 및 평가 기간을 수정합니다.")
    @ApiResponse(responseCode = "204", description = "상호평가 양식 수정 성공")
    ResponseEntity<Void> updateForm(
            @Parameter(description = "분반 ID", example = "1", required = true)
            @Positive @PathVariable Long sectionId,
            @Parameter(description = "양식 ID", example = "1", required = true)
            @Positive @PathVariable Long formId,
            @Valid @RequestBody PeerEvaluationFormUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication);

    @Operation(summary = "마일스톤 ID로 상호평가 양식 수정", description = "마일스톤 ID로 연결된 상호평가 양식의 익명 여부 및 평가 기간을 수정합니다.")
    @ApiResponse(responseCode = "204", description = "상호평가 양식 수정 성공")
    ResponseEntity<Void> updateFormByMilestoneId(
            @Parameter(description = "분반 ID", example = "1", required = true)
            @Positive @PathVariable Long sectionId,
            @Parameter(description = "마일스톤 ID", example = "1", required = true)
            @Positive @PathVariable Long milestoneId,
            @Valid @RequestBody PeerEvaluationFormUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication);
}
