package kgu.developers.admin.milestone.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneEvaluationWindowRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.request.RequiredArtifactRequest;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
import kgu.developers.admin.milestone.presentation.response.RequiredArtifactListResponse;
import kgu.developers.admin.milestone.presentation.response.RequiredArtifactPersistResponse;
import kgu.developers.domain.milestone.domain.MilestoneStatus;

@Tag(name = "관리자 마일스톤", description = "분반별 마일스톤 생성·조회·수정 API")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "관리자 권한 또는 담당 분반 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "마일스톤을 찾을 수 없음")
})
public interface MilestoneController {

    @Operation(summary = "마일스톤 생성")
    @ApiResponse(responseCode = "409", description = "같은 분반의 주차가 이미 사용 중임")
    ResponseEntity<MilestonePersistResponse> createMilestone(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Valid MilestoneCreateRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "분반별 마일스톤 목록 조회")
    ResponseEntity<MilestoneListResponse> getMilestones(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Parameter(description = "공개 상태 필터") MilestoneStatus status,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "마일스톤 상세 조회")
    ResponseEntity<MilestoneResponse> getMilestone(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Parameter(description = "마일스톤 ID", required = true) @Positive Long milestoneId,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "마일스톤 내용과 일정 수정")
    ResponseEntity<Void> updateMilestone(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Parameter(description = "마일스톤 ID", required = true) @Positive Long milestoneId,
            @Valid MilestoneUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "마일스톤 공개 상태 변경")
    ResponseEntity<Void> changeStatus(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Parameter(description = "마일스톤 ID", required = true) @Positive Long milestoneId,
            @Valid MilestoneStatusRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "마일스톤 평가 기간 수정")
    ResponseEntity<Void> updateEvaluationWindow(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Parameter(description = "마일스톤 ID", required = true) @Positive Long milestoneId,
            @Valid MilestoneEvaluationWindowRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "마일스톤 주차 일괄 변경",
            description = "주차는 표시·정렬 값만 변경하며 기존 제출·수정·평가 일정은 유지한다. "
                    + "일정 변경은 마일스톤 내용과 일정 수정 API 또는 평가 기간 수정 API를 사용한다."
    )
    @ApiResponse(responseCode = "409", description = "같은 분반의 주차가 이미 사용 중임")
    ResponseEntity<Void> updateWeekNumbers(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Valid MilestoneWeekNumbersRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "마일스톤 필수 산출물 목록 조회",
            description = "담당 분반 마일스톤의 활성 산출물 규칙을 조회한다. 삭제한 규칙은 기존 제출 이력에는 유지되지만 새 제출 검증과 목록에서는 제외된다."
    )
    ResponseEntity<RequiredArtifactListResponse> getRequiredArtifacts(
            @Positive Long sectionId,
            @Positive Long milestoneId,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "마일스톤 필수 산출물 생성",
            description = "유형은 FILE, LINK, TEXT, CHEERPJ_RUN 중 하나다. 허용 확장자와 최대 용량은 FILE 유형에서만 설정할 수 있다."
    )
    ResponseEntity<RequiredArtifactPersistResponse> createRequiredArtifact(
            @Positive Long sectionId,
            @Positive Long milestoneId,
            @Valid RequiredArtifactRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "마일스톤 필수 산출물 수정")
    ResponseEntity<Void> updateRequiredArtifact(
            @Positive Long sectionId,
            @Positive Long milestoneId,
            @Positive Long requiredArtifactId,
            @Valid RequiredArtifactRequest request,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "마일스톤 필수 산출물 삭제",
            description = "규칙을 소프트 삭제한다. 기존 제출 이력은 유지되고 이후 제출 검증에서는 제외된다."
    )
    ResponseEntity<Void> deleteRequiredArtifact(
            @Positive Long sectionId,
            @Positive Long milestoneId,
            @Positive Long requiredArtifactId,
            @Parameter(hidden = true) Authentication authentication
    );
}
