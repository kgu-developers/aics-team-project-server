package kgu.developers.admin.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;

@Builder
public record PreSurveyResponseAdminResponse(

        @Schema(description = "사전조사 응답 식별자", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "응답자 학번", example = "202012345", requiredMode = REQUIRED)
        String userId,

        @Schema(description = "희망 역할", example = "[\"BACKEND\", \"PM\"]", requiredMode = REQUIRED)
        JsonNode preferredRoles,

        @Schema(description = "주제 의견", example = "학사 일정 알림 서비스를 만들고 싶습니다")
        String topicOpinion,

        @Schema(description = "기타 의견", example = "금요일 오후에는 회의가 어렵습니다")
        String etcOpinion,

        @Schema(description = "제출일", example = "2026-08-21 14:00", requiredMode = REQUIRED)
        String submittedAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static PreSurveyResponseAdminResponse from(PreSurveyResponse response) {
        return PreSurveyResponseAdminResponse.builder()
                .id(response.getId())
                .userId(response.getUserId())
                .preferredRoles(response.getPreferredRoles())
                .topicOpinion(response.getTopicOpinion())
                .etcOpinion(response.getEtcOpinion())
                .submittedAt(response.getSubmittedAt().format(FORMATTER))
                .build();
    }
}
