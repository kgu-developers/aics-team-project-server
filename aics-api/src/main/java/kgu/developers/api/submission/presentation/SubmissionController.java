package kgu.developers.api.submission.presentation;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

import kgu.developers.api.submission.presentation.request.PresentationContentRequest;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionArtifactRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.PresentationContentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionMemberConsentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionDetailResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionListResponse;

@Tag(name = "Submission", description = "제출·이력 API")
public interface SubmissionController {

    @Operation(
        summary = "우리 팀 제출 조회 API",
        description = """
            Description : 이 마일스톤에 대한 우리 팀의 제출 정보를 조회한다. 아직 한 번도 제출한 적 없어도
            404가 아니라 not_submitted 상태로 조회된다(최초 조회 시 자동 생성).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionResponse.class)))
    ResponseEntity<SubmissionResponse> getMyTeamSubmission(@PathVariable Long milestoneId, Authentication authentication);

    @Operation(
        summary = "제출 상세 조회 API",
        description = """
            Description : 제출 상세와 지금 제출 가능한지(canSubmitNow), 대기 중인 피드백이 있는지(hasPendingReview)를 함께 반환한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionResponse.class)))
    ResponseEntity<SubmissionResponse> getSubmission(@PathVariable Long submissionId, Authentication authentication);

    @Operation(
        summary = "버전 목록 조회 API",
        description = """
            Description : 제출의 버전 이력을 최신순으로 조회한다(PR 히스토리 스타일 — 누가 언제 뭘 바꿨는지).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionVersionListResponse.class)))
    ResponseEntity<SubmissionVersionListResponse> getVersions(@PathVariable Long submissionId, Authentication authentication);

    @Operation(
        summary = "버전 상세 조회 API",
        description = """
            Description : 특정 버전의 상세와 그 버전에 포함된 아티팩트 목록(파일/링크/텍스트)을 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionVersionDetailResponse.class)))
    ResponseEntity<SubmissionVersionDetailResponse> getVersion(
        @PathVariable Long submissionId,
        @PathVariable int version,
        Authentication authentication
    );

    @Operation(
        summary = "제출/재제출 API",
        description = """
            Description : 새 버전을 제출한다(제출/수정/재제출 통합, multipart/form-data).
            제출 가능 기간(마감/지각제출기간/수정기간, 또는 팀별 조기오픈 조건) 밖이면 403.
            파일 아티팩트는 files 파트 + fileArtifactIds(같은 순서의 요구산출물 식별자 배열)로,
            링크·텍스트 아티팩트는 artifacts 파트(JSON 배열)로 보낸다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionResponse.class)))
    ResponseEntity<SubmissionResponse> submitVersion(
        @PathVariable Long submissionId,
        @RequestParam String description,
        @RequestParam(required = false) String changeNote,
        @RequestPart(value = "artifacts", required = false) @Valid List<SubmissionArtifactRequest> artifacts,
        @RequestParam(required = false) List<Long> fileArtifactIds,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    );

    @Operation(
        summary = "팀원 확인 현황 조회 API",
        description = """
            Description : 최종보고서 제출에 대해 팀원들이 얼마나 확인했는지 요약(확인 인원/전체 인원/본인 확인 여부)을 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionMemberConsentResponse.class)))
    ResponseEntity<SubmissionMemberConsentResponse> getMemberConsent(
        @PathVariable Long submissionId,
        Authentication authentication
    );

    @Operation(
        summary = "본인 확인 등록 API",
        description = """
            Description : 최종보고서를 확인했다고 등록한다(멱등 — 이미 지금 버전을 확인했으면 그대로 유지).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionMemberConsentResponse.class)))
    ResponseEntity<SubmissionMemberConsentResponse> confirmAsMember(
        @PathVariable Long submissionId,
        Authentication authentication
    );

    @Operation(
        summary = "본인 확인 취소 API",
        description = """
            Description : 등록했던 확인을 취소한다(멱등 — 확인한 적 없어도 그대로 성공).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionMemberConsentResponse.class)))
    ResponseEntity<SubmissionMemberConsentResponse> cancelConfirmation(
        @PathVariable Long submissionId,
        Authentication authentication
    );

    @Operation(
        summary = "단계 완료 처리 API",
        description = """
            Description : 팀장 전용. 최종보고서 마일스톤이면 팀원 전원(탈퇴자 제외) 확인이 끝나야 200,
            아니면 428을 반환한다. 그 외 마일스톤은 게이트 없이 바로 200.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionResponse.class)))
    ResponseEntity<SubmissionResponse> completeSubmission(@PathVariable Long submissionId, Authentication authentication);

    @Operation(
        summary = "교수 재오픈 API",
        description = """
            Description : 담당 교수 전용. 완료된 단계를 다시 열어서, 지정한 시각까지 팀이 재제출할 수 있게 한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SubmissionResponse.class)))
    ResponseEntity<SubmissionResponse> reopenSubmission(
        @PathVariable Long submissionId,
        @Valid @RequestBody SubmissionReopenRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "발표 공개자료 조회 API",
        description = """
            Description : 발표 마일스톤의 공개자료를 조회한다. 다른 팀도 열람 가능(로그인만 하면 됨), 수업시간 외에도 상시 열람 가능.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PresentationContentResponse.class)))
    ResponseEntity<PresentationContentResponse> getPresentationContent(@PathVariable Long submissionId, Authentication authentication);

    @Operation(
        summary = "발표 공개자료 작성/수정 API",
        description = """
            Description : 팀 전용(팀원만 수정 가능). 이미 있으면 덮어쓴다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PresentationContentResponse.class)))
    ResponseEntity<PresentationContentResponse> updatePresentationContent(
        @PathVariable Long submissionId,
        @RequestBody PresentationContentRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "발표순서 정렬 팀별 공개자료 목록 API",
        description = """
            Description : 발표순서로 정렬된 팀별 공개자료 목록을 조회한다. 이전/다음 네비게이션은 이 배열로 프론트에서 처리.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MilestonePresentationsResponse.class)))
    ResponseEntity<MilestonePresentationsResponse> getMilestonePresentations(@PathVariable Long milestoneId, Authentication authentication);

    @Operation(
        summary = "발표순서 일괄 지정 API",
        description = """
            Description : 담당 교수 전용. 드래그앤드롭으로 지정한 팀별 발표순서를 일괄 반영한다(순서 로직 없음, 임의 지정).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> assignPresentationOrder(
        @PathVariable Long milestoneId,
        @Valid @RequestBody PresentationOrderRequest request,
        Authentication authentication
    );
}
