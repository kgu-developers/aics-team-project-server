package kgu.developers.admin.preSurveyResponse.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;

@Tag(name = "AdminPreSurveyResponse", description = "관리자 사전조사 응답 조회 API")
public interface PreSurveyResponseAdminController {

    @Operation(
        summary = "담당 분반 사전조사 응답 목록 조회 API",
        description = """
            Description : 담당 교수가 분반 전체 학생의 사전조사 응답(희망 역할·주제 의견·기타 의견)을
                한 번에 조회한다. 아직 응답하지 않은 학생은 목록에 포함되지 않는다.
                호출자의 인증 식별자를 교수 학번으로 사용하므로 본인이 담당하는 분반만 조회할 수 있다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreSurveyResponseAdminListResponse.class)))
    ResponseEntity<PreSurveyResponseAdminListResponse> getResponsesBySection(
        @Parameter(description = "분반 식별자") @PathVariable Long sectionId,
        Authentication authentication
    );
}
