package kgu.developers.admin.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;

@Builder
public record PreSurveyResponseAdminListResponse(

        @Schema(description = "분반 학생 사전조사 응답 목록", requiredMode = REQUIRED)
        List<PreSurveyResponseAdminResponse> contents
) {

    public static PreSurveyResponseAdminListResponse from(List<PreSurveyResponse> responses) {
        return PreSurveyResponseAdminListResponse.builder()
                .contents(responses.stream().map(PreSurveyResponseAdminResponse::from).toList())
                .build();
    }
}
