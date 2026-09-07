package kgu.developers.admin.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;

@Builder
public record PreSurveyResponseAdminResponse(

        @Schema(description = "사전조사 응답 식별자", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "응답자 학번", example = "202012345", requiredMode = REQUIRED)
        String userId,

        @Schema(description = "응답자 이름", example = "김철수", requiredMode = REQUIRED)
        String userName,

        @Schema(description = "희망 역할", example = "[\"BACKEND\", \"PM\"]", requiredMode = REQUIRED)
        JsonNode preferredRoles,

        @Schema(description = "주제 의견", example = "학사 일정 알림 서비스를 만들고 싶습니다")
        String topicOpinion,

        @Schema(description = "기타 의견", example = "금요일 오후에는 회의가 어렵습니다")
        String etcOpinion,

        @Schema(description = "희망 조원 학번. 지목하지 않았으면 null", example = "202054321")
        String preferredPeerUserId,

        @Schema(description = "희망 조원 이름. 지목하지 않았으면 null", example = "이영희")
        String preferredPeerName,

        @Schema(description = "희망 조원 지목 상태(PENDING/ACCEPTED/REJECTED). 지목하지 않았으면 null", example = "ACCEPTED")
        PreferredPeerStatus preferredPeerStatus,

        @Schema(description = "서로 지목했거나 지목 대상이 수락하여 매칭된 경우 true", example = "true", requiredMode = REQUIRED)
        boolean mutual,

        @Schema(description = "제출일", example = "2026-08-21 14:00", requiredMode = REQUIRED)
        String submittedAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static PreSurveyResponseAdminResponse from(PreSurveyResponse response, String userName,
            String preferredPeerName, boolean mutual) {
        return PreSurveyResponseAdminResponse.builder()
                .id(response.getId())
                .userId(response.getUserId())
                .userName(userName)
                .preferredRoles(response.getPreferredRoles())
                .topicOpinion(response.getTopicOpinion())
                .etcOpinion(response.getEtcOpinion())
                .preferredPeerUserId(response.getPreferredPeerUserId())
                .preferredPeerName(preferredPeerName)
                .preferredPeerStatus(response.getPreferredPeerStatus())
                .mutual(mutual)
                .submittedAt(response.getSubmittedAt().format(FORMATTER))
                .build();
    }
}
