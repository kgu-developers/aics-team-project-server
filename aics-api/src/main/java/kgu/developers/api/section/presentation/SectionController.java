package kgu.developers.api.section.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.section.presentation.response.SectionListResponse;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Section", description = "OOP 분반 API")
public interface SectionController {

    @Operation(summary = "내 분반 목록 조회 API", description = """
            - Description : 이 API는 액세스 토큰의 학번을 기준으로 본인이 담당하는 분반 목록을 조회합니다.
            - 강좌 상태/학년도/학기는 선택값이며, 생략하면 해당 조건으로는 거르지 않습니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionListResponse.class)))
    ResponseEntity<SectionListResponse> getMySections(
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

    @Operation(summary = "분반 상세 조회 API", description = """
            - Description : 이 API는 지정된 분반의 상세 정보를 조회합니다.
        """)
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = SectionResponse.class)))
    ResponseEntity<SectionResponse> getSectionById(
        @Parameter(
            description = "분반 ID는 URL 경로 변수 입니다.",
            example = "1",
            required = true
        ) @Positive @PathVariable Long sectionId
    );
}
