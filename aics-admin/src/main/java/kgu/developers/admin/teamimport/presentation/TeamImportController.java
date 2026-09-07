package kgu.developers.admin.teamimport.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.teamimport.presentation.response.TeamImportApplyResponse;
import kgu.developers.admin.teamimport.presentation.response.TeamImportPreviewResponse;

@Tag(name = "TeamImport", description = "팀 명단 엑셀 업로드 API")
public interface TeamImportController {

    @Operation(summary = "팀 명단 업로드 미리보기 API", description = """
            - Description : 이 API는 팀 명단 엑셀을 검증만 하고 저장해 두며, 아직 팀을 만들지 않습니다.
            - 첫 시트에서 "학번" 헤더가 있는 행을 찾아 그 아래를 명단으로 읽고, 컬럼 위치는 헤더 이름으로 찾습니다.
              (팀명·학번 필수 / 성명·이름, 팀장, 역할, 전화번호, 학년은 선택. 팀장 칸은 Y·O·1·팀장 중 아무 값이면 팀장으로 봅니다)
            - 전화번호 헤더는 "전화번호·연락처·폰·전화", 학년 헤더는 "학년"으로 인식합니다. 두 열이 없는 기존 양식도 그대로 읽힙니다.
            - 전화번호는 하이픈이 있어도 없어도 되고(01012345678, 010-1234-5678, 02-123-4567 모두 허용)
              010-1234-5678 형식으로 맞춰 저장합니다. 학년은 1~4 또는 "3학년" 표기를 허용합니다.
            - 두 열은 비워 두면 기존 값을 그대로 둡니다(빈 칸은 "값 없음"이지 "지우기"가 아닙니다).
            - 행 상태는 VALID(편성 예정), UPDATE(팀장·역할·전화번호·학년 갱신 예정),
              DUPLICATE(이미 같은 팀에 있어 건너뜀), INVALID(오류) 입니다.
            - 해당 분반에 수강 등록되지 않은 학생, 이미 다른 팀에 편성된 학생, 팀장이 둘인 팀,
              전화번호·학년 형식이 맞지 않는 행은 INVALID 입니다.
            - INVALID 행이 하나라도 있으면 반영 API가 거부되므로 파일을 고쳐 다시 업로드해야 합니다.
            - 미리보기는 30분 뒤 만료됩니다. 해당 분반의 조교·담당 교수 또는 전역 관리자만 호출할 수 있습니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = TeamImportPreviewResponse.class)))
    ResponseEntity<TeamImportPreviewResponse> preview(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId,
        @Parameter(
            description = "팀 명단 엑셀 파일 (.xlsx, .xls)",
            required = true
        ) MultipartFile file
    );

    @Operation(summary = "팀 명단 반영 API", description = """
            - Description : 이 API는 미리보기 결과의 VALID 행으로 팀을 만들고 팀원을 편성합니다.
            - 같은 분반에 같은 이름의 팀이 이미 있으면 그 팀에 편성하고, 없으면 형성중(FORMING) 상태로 새로 만듭니다.
            - 이미 편성된 팀원은 건너뛰며, 이미 반영했거나 만료된 업로드는 반영할 수 없습니다.
            - 전화번호는 계정 연락처(User.phone)를, 학년은 분반 수강 정보를 갱신합니다. 값이 있는 칸만 반영합니다.
            - 해당 분반의 조교·담당 교수 또는 전역 관리자만 호출할 수 있습니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = TeamImportApplyResponse.class)))
    ResponseEntity<TeamImportApplyResponse> apply(
        @Parameter(
            description = "업로드 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long importId
    );
}
