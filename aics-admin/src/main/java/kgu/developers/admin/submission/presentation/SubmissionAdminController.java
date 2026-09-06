package kgu.developers.admin.submission.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

import kgu.developers.admin.submission.presentation.response.SubmissionAdminListResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminDetailResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminListResponse;

@Tag(name = "AdminSubmission", description = "관리자 제출·이력 조회 API")
public interface SubmissionAdminController {

    @Operation(
        summary = "담당 분반 팀별 제출 현황 조회 API",
        description = """
            Description : 담당 교수가 이 마일스톤에 대한 담당 분반 팀들의 제출 현황을 한 번에 조회한다.
                아직 한 번도 제출하지 않은 팀도 not_submitted 상태로 함께 조회된다(최초 조회 시 자동 생성).
                호출자의 인증 식별자를 교수 학번으로 사용하므로 본인이 담당하는 분반의 마일스톤만 조회할 수 있다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionAdminListResponse.class)))
    ResponseEntity<SubmissionAdminListResponse> getSubmissionsByMilestone(
        @Parameter(description = "마일스톤 식별자") @PathVariable Long milestoneId,
        Authentication authentication
    );

    @Operation(
        summary = "제출 상세 조회 API",
        description = """
            Description : 담당 교수가 특정 제출의 상세와 지금 제출 가능한지(canSubmitNow), 대기 중인
                피드백이 있는지(hasPendingReview)를 함께 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionAdminResponse.class)))
    ResponseEntity<SubmissionAdminResponse> getSubmission(
        @Parameter(description = "제출 식별자") @PathVariable Long submissionId,
        Authentication authentication
    );

    @Operation(
        summary = "버전 목록(제출 이력) 조회 API",
        description = """
            Description : 담당 교수가 제출의 버전 이력을 최신순으로 조회한다(PR 히스토리 스타일 — 누가 언제 뭘 바꿨는지).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionVersionAdminListResponse.class)))
    ResponseEntity<SubmissionVersionAdminListResponse> getVersions(
        @Parameter(description = "제출 식별자") @PathVariable Long submissionId,
        Authentication authentication
    );

    @Operation(
        summary = "버전 상세 조회 API",
        description = """
            Description : 담당 교수가 특정 버전의 상세와 그 버전에 포함된 아티팩트 목록(파일 다운로드
                URL·링크·텍스트)을 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionVersionAdminDetailResponse.class)))
    ResponseEntity<SubmissionVersionAdminDetailResponse> getVersion(
        @Parameter(description = "제출 식별자") @PathVariable Long submissionId,
        @Parameter(description = "버전 번호") @PathVariable int version,
        Authentication authentication
    );
}
