package kgu.developers.admin.sectionartifact.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminListResponse;

@Tag(name = "AdminSectionArtifact", description = "관리자 분반 산출물 내보내기 API")
public interface SectionArtifactAdminController {

    @Operation(
        summary = "분반 팀별 산출물 요약 조회 API",
        description = """
            Description : 담당 교수가 담당 분반의 팀별 산출물 요약(회의록 수, 회의록 수정 로그 수,
                제출 이력 단계 수, 마감된 미제출 단계 수)을 조회한다. 엑셀 다운로드의 "팀별 요약"
                시트와 같은 집계이며, 기준일(asOf, 생략하면 KST 기준 오늘)의 종료 시각까지 쌓인
                이력만 센다. 회의록 수정 로그 수는 회의록의 낙관적 락 version 합으로 구한 근사값이라
                기준일 이후의 수정도 포함된다. 팀·팀원·마일스톤 정보와 삭제 여부는 현재 데이터
                기준이며 완전한 과거 스냅샷은 아니다. 담당하지 않는 분반은 403이다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionArtifactSummaryAdminListResponse.class)))
    @ApiResponse(responseCode = "403", description = "담당 분반이 아님")
    ResponseEntity<SectionArtifactSummaryAdminListResponse> getArtifactSummary(
        @Parameter(description = "분반 식별자") @PathVariable Long sectionId,
        @Parameter(description = "집계 기준일(yyyy-MM-dd, 생략하면 오늘)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
        Authentication authentication
    );

    @Operation(
        summary = "분반 산출물 현황 엑셀 다운로드 API",
        description = """
            Description : 담당 교수가 담당 분반의 팀별 산출물 현황을 엑셀(xlsx) 파일 하나로 내려받는다.
                기준일(asOf, 생략하면 KST 기준 오늘)의 종료 시각까지 쌓인 이력만 집계하며, 시트는
                "팀별 요약"과 "단계별 제출 현황" 두 장이다. 상태 열은 조회 시점의 최신 제출 상태
                원본 enum이라 과거 기준일로 조회해도 그 시점 상태를 복원하지 않는다. 회의록 수정 로그
                수는 회의록의 낙관적 락 version 합으로 구한 근사값이라 기준일 이후의 수정도 포함된다.
                중간보고서 제출 시각은 저장된 마지막 제출 시각이며 최초 제출 이력은 보존하지 않는다.
                팀·팀원·마일스톤 정보와 삭제 여부는 현재 데이터 기준이다. 담당하지 않는 분반은 403이다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200",
        content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @ApiResponse(responseCode = "403", description = "담당 분반이 아님")
    ResponseEntity<byte[]> downloadArtifactsExcel(
        @Parameter(description = "분반 식별자") @PathVariable Long sectionId,
        @Parameter(description = "집계 기준일(yyyy-MM-dd, 생략하면 오늘)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
        Authentication authentication
    );
}
