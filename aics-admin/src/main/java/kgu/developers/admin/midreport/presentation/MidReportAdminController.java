package kgu.developers.admin.midreport.presentation;

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
import kgu.developers.admin.midreport.presentation.request.MidReportFeedbackAdminRequest;
import kgu.developers.admin.midreport.presentation.response.MidReportAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminPageResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminResponse;
import kgu.developers.common.exception.ExceptionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AdminMidReport", description = "관리자 중간보고서 및 중간점검 피드백 API")
public interface MidReportAdminController {

    @Operation(
        summary = "관리자 특정 팀 중간보고서 상세 조회 API",
        description = """
            Description : 담당 교수가 특정 팀의 중간보고서 상세 내용(4대 블록: 주제, GUI 화면 및 이미지, 엔진 설계, 진행 계획)과
                제출 상태, 최종 제출자, 버전 정보를 조회한다.
                GUI 화면에 등록된 이미지는 presigned URL로 변환되어 즉시 조회가 가능하다.
            Assignee : 황호찬
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MidReportAdminResponse.class))),
        @ApiResponse(responseCode = "403", description = "담당 분반이 아니거나 권한 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "팀 또는 마일스톤을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<MidReportAdminResponse> getMidReport(
        @Parameter(description = "분반 식별자") @PathVariable @Positive Long sectionId,
        @Parameter(description = "팀 식별자") @PathVariable @Positive Long teamId,
        Authentication authentication
    );

    @Operation(
        summary = "관리자 중간 점검 피드백 등록 API",
        description = """
            Description : 담당 교수가 특정 팀의 중간보고서에 피드백을 등록한다.
                등록된 피드백은 해당 팀의 팀 메시지함(TeamMessage)에 relatedType=MID_REPORT 로 저장되어
                향후 최종 채점 시 피드백 이력을 모아볼 수 있다.
                피드백이 등록되면 중간보고서의 상태가 REVISION_REQUESTED(수정 요청)로 리오픈되어 학생들의 재제출이 가능해진다.
            Assignee : 황호찬
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MidReportFeedbackAdminResponse.class))),
        @ApiResponse(responseCode = "403", description = "담당 분반이 아니거나 권한 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "중간보고서를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<MidReportFeedbackAdminResponse> postFeedback(
        @Parameter(description = "분반 식별자") @PathVariable @Positive Long sectionId,
        @Parameter(description = "팀 식별자") @PathVariable @Positive Long teamId,
        @Valid @RequestBody MidReportFeedbackAdminRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "관리자 중간 점검 피드백 이력 조회 API",
        description = """
            Description : 담당 교수가 특정 팀의 중간보고서에 등록된 피드백(팀 메시지 중 MID_REPORT 타입) 이력을 최신순으로 조회한다.
                채점 시 지금까지 제공된 피드백들을 한 번에 모아볼 수 있다.
            Assignee : 황호찬
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MidReportFeedbackAdminPageResponse.class))),
        @ApiResponse(responseCode = "403", description = "담당 분반이 아니거나 권한 없음", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<MidReportFeedbackAdminPageResponse> getFeedbacks(
        @Parameter(description = "분반 식별자") @PathVariable @Positive Long sectionId,
        @Parameter(description = "팀 식별자") @PathVariable @Positive Long teamId,
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @Parameter(description = "페이지 크기(최대 100)", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) int size,
        Authentication authentication
    );
}
