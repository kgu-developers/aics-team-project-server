package kgu.developers.admin.proposal.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kgu.developers.admin.proposal.presentation.request.ProposalFeedbackAdminRequest;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminPageResponse;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminResponse;
import kgu.developers.common.exception.ExceptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AdminProposal", description = "관리자 제안서 피드백 API")
public interface ProposalAdminController {

    @Operation(
        summary = "관리자 제안서 피드백 등록 API",
        description = """
            Description : 담당 교수가 특정 팀의 제안서에 피드백을 등록한다.
                등록된 피드백은 해당 팀의 팀 메시지함(TeamMessage)에 relatedType=PROPOSAL 로 저장된다.
                이미 제안이 완료된(팀원 동의가 끝난) 제안서라면 상태가 REVISION_REQUESTED(수정 요청)로
                리오픈되고 기존 동의는 무효화되어 학생들의 수정·재동의가 가능해진다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProposalFeedbackAdminResponse.class))),
        @ApiResponse(responseCode = "403", description = "담당 분반이 아니거나 권한 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "팀 또는 제안서를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<ProposalFeedbackAdminResponse> postFeedback(
        @Parameter(description = "분반 식별자") @PathVariable @Positive Long sectionId,
        @Parameter(description = "팀 식별자") @PathVariable @Positive Long teamId,
        @Valid @RequestBody ProposalFeedbackAdminRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "관리자 제안서 피드백 이력 조회 API",
        description = """
            Description : 담당 교수가 특정 팀의 제안서에 등록된 피드백(팀 메시지 중 PROPOSAL 타입) 이력을 최신순으로 조회한다.
                제안서나 팀 메시지함이 아직 없으면 빈 목록을 반환한다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProposalFeedbackAdminPageResponse.class))),
        @ApiResponse(responseCode = "403", description = "담당 분반이 아니거나 권한 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<ProposalFeedbackAdminPageResponse> getFeedbacks(
        @Parameter(description = "분반 식별자") @PathVariable @Positive Long sectionId,
        @Parameter(description = "팀 식별자") @PathVariable @Positive Long teamId,
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @Parameter(description = "페이지 크기(최대 100)", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) int size,
        Authentication authentication
    );
}
