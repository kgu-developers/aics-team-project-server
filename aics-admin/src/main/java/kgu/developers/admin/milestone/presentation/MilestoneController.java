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
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
import kgu.developers.domain.milestone.domain.MilestoneStatus;

@Tag(name = "관리자 마일스톤", description = "분반별 마일스톤 생성·조회·수정 API")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "교수자 권한 또는 분반 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "마일스톤을 찾을 수 없음")
})
public interface MilestoneController {

    @Operation(summary = "마일스톤 생성")
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

    @Operation(summary = "마일스톤 주차 일괄 변경")
    ResponseEntity<Void> updateWeekNumbers(
            @Parameter(description = "분반 ID", required = true) @Positive Long sectionId,
            @Valid MilestoneWeekNumbersRequest request,
            @Parameter(hidden = true) Authentication authentication
    );
}
