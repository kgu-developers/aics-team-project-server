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
                기준이며 완전한 과거 스냅샷은 아니다. 중간점검을 기준일 이후 재제출했더라도
                기준일 이전 반려로 제출 이력이 확인되면 제출 단계 수에 포함하고 미제출로 세지 않는다.
                담당하지 않는 분반은 403이다.
            Assignee : 이석민
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
                그래서 반려 후 재제출한 중간점검은 최초 제출 시각을 알 수 없어 "최초 제출 지각 여부"를
                빈 칸으로 둔다(반려 이력이 없는 중간점검은 그 값이 곧 최초 제출이라 지각 여부를 채운다).
                기준일 이후 재제출한 중간점검은 기준일 이전 반려가 확인되면 REVISION_REQUESTED로
                표시하며 제출 단계 수에 포함하고 미제출로 세지 않는다. 이 경우 복원할 수 없는
                첫 제출·최신 제출·최신 버전·지각 여부는 빈 칸으로 둔다.
                "최신 파일 수"·"최신 이미지 수"는 최신 제출 버전 기준이고, "전체 파일 용량"은 기준일까지의
                전 버전을 누적한 값이다(소프트 삭제된 파일은 어느 쪽에서도 빠진다).
                팀·팀원·마일스톤 정보와 삭제 여부는 현재 데이터 기준이다. 담당하지 않는 분반은 403이다.
            Assignee : 이석민
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
