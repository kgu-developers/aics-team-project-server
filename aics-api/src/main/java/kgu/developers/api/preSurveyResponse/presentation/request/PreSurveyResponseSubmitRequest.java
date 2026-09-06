package kgu.developers.api.preSurveyResponse.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record PreSurveyResponseSubmitRequest(

    @Schema(description = "희망 역할 목록(선호 순서대로)", example = "[\"BACKEND\", \"PM\"]", requiredMode = REQUIRED)
    @NotEmpty
    List<@NotBlank String> preferredRoles,

    @Schema(description = "주제 의견", example = "학사 일정 알림 서비스를 만들고 싶습니다")
    String topicOpinion,

    @Schema(description = "기타 의견", example = "금요일 오후에는 회의가 어렵습니다")
    String etcOpinion,

    @Schema(description = "조원으로 희망하는 학생 학번(1명). 지목하지 않거나 취소하려면 null", example = "202054321")
    @Size(max = 20)
    String preferredPeerUserId
) {
}
