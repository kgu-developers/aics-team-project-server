package kgu.developers.api.midreport.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.midreport.presentation.request.MidReportBlockCompletionRequest;
import kgu.developers.api.midreport.presentation.request.MidReportBlockUpdateRequest;
import kgu.developers.api.midreport.presentation.request.MidReportSubmissionRequest;
import kgu.developers.api.midreport.presentation.response.MidReportResponse;
import kgu.developers.common.exception.ExceptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "MidReport", description = "중간보고서 공동 작성 API")
public interface MidReportController {
    @Operation(
        summary = "현재 팀 중간보고서 조회",
        description = "활성 STUDENT 수강 상태와 팀 소속을 검증하고, 게시된 MID_REPORT 마일스톤의 문서를 최초 조회 시 생성합니다."
    )
    ResponseEntity<MidReportResponse> getCurrent(Authentication authentication);

    @Operation(
        summary = "중간보고서 영역 저장",
        description = "고정 영역 키와 필드 구조를 검증해 저장합니다. 완료 영역을 수정하면 IN_PROGRESS로 돌아가며, 제출된 문서는 읽기 전용입니다."
            + " GUI 화면 이미지는 기존 파일 업로드 API의 imageFileId를 사용하고, 현재 팀원이 올린 이미지 파일만 연결할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "저장 성공"),
        @ApiResponse(responseCode = "400", description = "고정 필드 형식 오류", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "409", description = "VERSION_CONFLICT 또는 제출된 문서", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<MidReportResponse> updateBlock(
        @PathVariable Long id,
        @PathVariable String blockKey,
        @Valid @RequestBody MidReportBlockUpdateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "중간보고서 영역 완료",
        description = "topic(title, description), gui-design(guiScreens), engine-design(features, architecture, testCases), project-plan(completed, inProgress, remaining, help)의 필수값을 검증합니다. "
            + "guiScreens JSON 행은 id/name/description 문자열을 모두 가져야 하며 imageFileId는 선택 사항입니다. "
            + "조회 응답에는 유효한 이미지의 imageName과 만료되는 imageUrl이 함께 제공됩니다. "
            + "testCases JSON 행은 id/description/input/output 문자열을 모두 가져야 합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "완료 처리 성공"),
        @ApiResponse(responseCode = "409", description = "VERSION_CONFLICT 또는 제출된 문서", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "422", description = "필수 필드 또는 JSON 행 미완성", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<MidReportResponse> completeBlock(
        @PathVariable Long id,
        @PathVariable String blockKey,
        @Valid @RequestBody MidReportBlockCompletionRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "중간보고서 최종 제출",
        description = "팀원 승인 없이 현재 활성 학생인 팀장만 제출할 수 있습니다. 네 영역이 모두 COMPLETED여야 하며, "
            + "미완료 영역은 현재 문서 조회 응답의 blocks[].status로 확인합니다. 제출 후 문서는 읽기 전용입니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "제출 성공"),
        @ApiResponse(responseCode = "403", description = "팀장이 아니거나 활성 학생이 아님", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "409", description = "VERSION_CONFLICT 또는 이미 제출된 문서", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "422", description = "완료되지 않은 영역 존재", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    ResponseEntity<MidReportResponse> submit(
        @PathVariable Long id,
        @Valid @RequestBody MidReportSubmissionRequest request,
        Authentication authentication
    );
}
