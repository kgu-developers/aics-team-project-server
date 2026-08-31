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
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Peer Evaluation Form", description = "상호평가 양식 API")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 또는 담당 분반 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "분반 또는 마일스톤을 찾을 수 없음")
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
}
