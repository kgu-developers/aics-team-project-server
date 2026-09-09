package kgu.developers.admin.importstatus.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.importstatus.presentation.response.RosterImportStatusResponse;

@Tag(name = "RosterImportStatus", description = "분반별 명단 반영 현황 API")
public interface RosterImportStatusController {

    @Operation(summary = "분반별 명단 마지막 반영 현황 조회 API", description = """
        - Description : 수강생 명단과 팀 명단의 마지막 apply 성공 파일명·반영 시각을 조회합니다.
        - 반영 성공 이력이 없는 유형은 null이며, preview만 수행했거나 apply에 실패한 건은 포함하지 않습니다.
        - 업로드 횟수 제한은 없습니다. 재업로드는 기존 기록을 덮어쓰지 않고 새 ImportBatch로 보관합니다.
        - 화면에는 각 유형의 마지막 성공 반영 건만 노출합니다.
        - 해당 분반의 조교·담당 교수 또는 전역 관리자만 호출할 수 있습니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = RosterImportStatusResponse.class)))
    ResponseEntity<RosterImportStatusResponse> getStatus(
        @Parameter(description = "분반 식별자", example = "1", required = true)
        @Positive @PathVariable Long sectionId
    );
}
