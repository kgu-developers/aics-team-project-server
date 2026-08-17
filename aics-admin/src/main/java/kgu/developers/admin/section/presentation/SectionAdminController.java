package kgu.developers.admin.section.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.enrollment.presentation.request.EnrollmentAdminRequest;
import kgu.developers.admin.enrollment.presentation.request.EnrollmentAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionContactVisibilityUpdateRequest;
import kgu.developers.admin.enrollment.presentation.response.EnrollmentAdminListResponse;
import kgu.developers.admin.enrollment.presentation.response.EnrollmentAdminPersistResponse;
import kgu.developers.admin.enrollment.presentation.response.EnrollmentAdminResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminListResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminPersistResponse;
import kgu.developers.admin.section.presentation.response.SectionAdminResponse;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Section", description = "OOP 분반 API")
public interface SectionAdminController {

    @Operation(summary = "분반 생성 API", description = """
            - Description : 이 API는 강좌에 속한 신규 분반을 생성합니다.
        """)
    @ApiResponse(
        responseCode = "201",
        content = @Content(schema = @Schema(implementation = SectionAdminPersistResponse.class)))
    ResponseEntity<SectionAdminPersistResponse> createSection(
        @Parameter(
            description = "분반 생성 request 객체 입니다.",
            required = true
        ) @Valid @RequestBody SectionAdminRequest request
    );

    @Operation(summary = "분반 단건 조회 API", description = """
            - Description : 이 API는 지정된 분반 하나를 조회합니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionAdminResponse.class)))
    ResponseEntity<SectionAdminResponse> getSectionById(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId
    );

    @Operation(summary = "강좌별 분반 목록 조회 API", description = """
            - Description : 이 API는 지정된 강좌의 분반 목록을 조회합니다.
            - courseId 쿼리 파라미터가 있을 때만 이 API로 라우팅됩니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionAdminListResponse.class)))
    ResponseEntity<SectionAdminListResponse> getSectionsByCourseId(
        @Parameter(
            description = "강좌 ID 입니다.",
            example = "1",
            required = true
        ) @Positive @RequestParam Long courseId
    );

    @Operation(summary = "교수별 분반 목록 조회 API", description = """
            - Description : 이 API는 지정된 교수가 담당하는 분반 목록을 조회합니다.
            - professorId 쿼리 파라미터가 있을 때만 이 API로 라우팅됩니다.
            - 강좌 상태/학년도/학기는 선택값이며, 생략하면 해당 조건으로는 거르지 않습니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionAdminListResponse.class)))
    ResponseEntity<SectionAdminListResponse> getSectionsByProfessorId(
        @Parameter(
            description = "교수 학번 입니다.",
            example = "202699999",
            required = true
        ) @NotBlank @RequestParam String professorId,
        @Parameter(
            description = "강좌 상태 입니다.",
            example = "ACTIVE"
        ) @RequestParam(required = false) StatusType status,
        @Parameter(
            description = "학년도 입니다.",
            example = "2026"
        ) @RequestParam(required = false) Integer year,
        @Parameter(
            description = "학기 입니다.",
            example = "SPRING"
        ) @RequestParam(required = false) SemesterType semester
    );

    @Operation(summary = "분반 수강생/조교 등록 API", description = """
            - Description : 이 API는 지정된 분반에 수강생 또는 조교를 한 명 등록합니다.
            - 이미 등록된 사용자면 409를 응답합니다. 탈퇴(소프트삭제)한 이력은 중복으로 보지 않습니다.
        """)
    @ApiResponse(
        responseCode = "201",
        content = @Content(schema = @Schema(implementation = EnrollmentAdminPersistResponse.class)))
    ResponseEntity<EnrollmentAdminPersistResponse> createEnrollment(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId,
        @Parameter(
            description = "수강생 등록 request 객체 입니다.",
            required = true
        ) @Valid @RequestBody EnrollmentAdminRequest request
    );

    @Operation(summary = "분반 수강생 명단 조회 API", description = """
            - Description : 이 API는 지정된 분반의 수강생 명단을 학번 오름차순으로 조회합니다.
            - 탈퇴한 수강생도 status로 구분되어 함께 반환됩니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = EnrollmentAdminListResponse.class)))
    ResponseEntity<EnrollmentAdminListResponse> getEnrollmentsBySectionId(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId
    );

    @Operation(summary = "분반 수강생 역할/상태 변경 API", description = """
            - Description : 이 API는 분반에 등록된 사용자의 역할 또는 상태를 부분 수정합니다.
            - 보낸 필드만 반영되고 생략한 필드는 그대로 유지됩니다.
            - 상태는 ACTIVE(수강중)와 WITHDRAWN(탈퇴)를 오갈 수 있습니다. 명단에서 완전히 빼려면 별도 삭제가 필요합니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = EnrollmentAdminResponse.class)))
    ResponseEntity<EnrollmentAdminResponse> updateEnrollment(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId,
        @Parameter(
            description = "학번은 URL 경로 변수 입니다.",
            example = "202699999",
            required = true
        ) @NotBlank @PathVariable String studentNumber,
        @Parameter(
            description = "수강생 역할/상태 수정 request 객체 입니다. 수정할 필드만 담습니다.",
            required = true
        ) @Valid @RequestBody EnrollmentAdminUpdateRequest request
    );

    @Operation(summary = "분반 팀 목록 조회 API", description = """
            - Description : 이 API는 지정된 분반의 팀 목록을 팀명 오름차순으로 조회합니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = TeamAdminListResponse.class)))
    ResponseEntity<TeamAdminListResponse> getTeamsBySectionId(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId
    );

    @Operation(summary = "분반 수정 API", description = """
            - Description : 이 API는 기존 분반 정보를 부분 수정합니다. 보낸 필드만 반영되고 생략한 필드는 그대로 유지됩니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionAdminResponse.class)))
    ResponseEntity<SectionAdminResponse> updateSection(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId,
        @Parameter(
            description = "분반 수정 request 객체 입니다. 수정할 필드만 담습니다.",
            required = true
        ) @Valid @RequestBody SectionAdminUpdateRequest request
    );

    @Operation(summary = "분반 연락처 공개기간 수정 API", description = """
            - Description : 이 API는 분반의 연락처 공개기간을 보낸 값으로 그대로 덮어씁니다.
            - 두 값을 모두 생략(null)하면 공개기간이 해제됩니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionAdminResponse.class)))
    ResponseEntity<SectionAdminResponse> updateSectionContactVisibility(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId,
        @Parameter(
            description = "연락처 공개기간 수정 request 객체 입니다.",
            required = true
        ) @Valid @RequestBody SectionContactVisibilityUpdateRequest request
    );

    @Operation(summary = "분반 삭제 API", description = """
            - Description : 이 API는 지정된 분반을 삭제합니다.
        """)
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> deleteSection(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId
    );
}
