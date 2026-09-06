package kgu.developers.api.preSurveyResponse.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyClassmateListResponse;
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
                          조원으로 희망하는 학생 1명을 preferredPeerUserId 로 함께 지목한다. null 로 다시 제출하면 지목이 취소되고,
                          다른 학생으로 바꾸면 지목 상태가 다시 PENDING 이 된다. 대상이 그대로면 상대의 수락·거절 결과는 유지된다.
                          지목 상태는 (PENDING, ACCEPTED, REJECTED)이다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreSurveyResponseDetailResponse.class)))
    ResponseEntity<PreSurveyResponseDetailResponse> submit(
        @Positive @PathVariable Long sectionId,
        @Valid @RequestBody PreSurveyResponseSubmitRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "내 사전조사 응답 조회 API",
        description = """
            Description : 인증된 사용자가 해당 분반에 제출한 사전조사 응답을 조회한다. 제출한 응답이 없으면 404 를 반환한다.
                          지목 상태는 (PENDING, ACCEPTED, REJECTED)이다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreSurveyResponseDetailResponse.class)))
    ResponseEntity<PreSurveyResponseDetailResponse> getMyResponse(
        @Positive @RequestParam Long sectionId,
        Authentication authentication
    );

    @Operation(
        summary = "지목 가능한 같은 분반 학생 검색 API",
        description = """
            Description : 조원으로 지목할 수 있는 같은 분반 수강생을 이름 또는 학번으로 검색한다. 본인과 수강 중이 아닌 사람은 제외된다.
                          keyword 를 비우면 분반 전체 명단을 반환한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreSurveyClassmateListResponse.class)))
    ResponseEntity<PreSurveyClassmateListResponse> searchClassmates(
        @Positive @PathVariable Long sectionId,
        @RequestParam(required = false) String keyword,
        Authentication authentication
    );
}
