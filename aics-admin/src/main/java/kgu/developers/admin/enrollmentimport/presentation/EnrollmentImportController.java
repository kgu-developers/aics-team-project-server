package kgu.developers.admin.enrollmentimport.presentation;

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
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportApplyResponse;
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportPreviewResponse;

@Tag(name = "EnrollmentImport", description = "수강생 명단 엑셀 업로드 API")
public interface EnrollmentImportController {

    @Operation(summary = "수강생 명단 업로드 미리보기 API", description = """
            - Description : 이 API는 수강생 명단 엑셀을 검증만 하고 저장해 두며, 아직 수강 정보를 반영하지 않습니다.
            - 첫 시트에서 "학번" 헤더가 있는 행을 찾아 그 아래를 명단으로 읽고, 컬럼 위치는 헤더 이름으로 찾습니다.
              (학번 필수 / 성명·이름, 이메일, 연락처, 역할은 선택. 역할이 없으면 학생, 이메일이 없으면 학번@kyonggi.ac.kr)
            - 행 상태는 VALID(가입된 학생, 등록 예정), NEW_USER(미가입 학생, 계정 생성 후 등록 예정),
              DUPLICATE(이미 등록되어 건너뜀), INVALID(오류) 입니다.
            - INVALID 행이 하나라도 있으면 반영 API가 거부되므로 파일을 고쳐 다시 업로드해야 합니다.
            - 미리보기는 30분 뒤 만료됩니다. 관리자 권한(ROLE_ADMIN)이 필요합니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = EnrollmentImportPreviewResponse.class)))
    ResponseEntity<EnrollmentImportPreviewResponse> preview(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId,
        @Parameter(
            description = "수강생 명단 엑셀 파일 (.xlsx, .xls)",
            required = true
        ) MultipartFile file
    );

    @Operation(summary = "수강생 명단 반영 API", description = """
            - Description : 이 API는 미리보기 결과의 VALID/NEW_USER 행을 실제 수강 정보로 반영합니다.
            - NEW_USER 행은 계정을 함께 만듭니다. 초기 비밀번호는 학번이며, 학생이 로그인 후 비밀번호 변경 API로 바꿔야 합니다.
            - 이미 등록된 수강생은 건너뛰며, 이미 반영했거나 만료된 업로드는 반영할 수 없습니다.
            - 관리자 권한(ROLE_ADMIN)이 필요합니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = EnrollmentImportApplyResponse.class)))
    ResponseEntity<EnrollmentImportApplyResponse> apply(
        @Parameter(
            description = "업로드 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long importId
    );
}
