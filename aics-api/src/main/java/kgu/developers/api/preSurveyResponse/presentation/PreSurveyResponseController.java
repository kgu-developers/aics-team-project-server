package kgu.developers.api.preSurveyResponse.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyResponseDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "PreSurveyResponse", description = "사전조사 응답 API")
public interface PreSurveyResponseController {

    @Operation(
        summary = "사전조사 응답 제출 API",
        description = """
            Description : 분반 사전조사에 응답한다. 응답자는 요청 값이 아니라 인증된 사용자로 기록되며, 해당 분반 수강생만 제출할 수 있다.
                          이미 제출한 응답이 있으면 새 행을 만들지 않고 기존 응답을 덮어쓴다(재제출).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreSurveyResponseDetailResponse.class)))
    ResponseEntity<PreSurveyResponseDetailResponse> submit(
        @PathVariable Long sectionId,
        @Valid @RequestBody PreSurveyResponseSubmitRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "내 사전조사 응답 조회 API",
        description = """
            Description : 인증된 사용자가 해당 분반에 제출한 사전조사 응답을 조회한다. 제출한 응답이 없으면 404 를 반환한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreSurveyResponseDetailResponse.class)))
    ResponseEntity<PreSurveyResponseDetailResponse> getMyResponse(
        @RequestParam Long sectionId,
        Authentication authentication
    );
}
