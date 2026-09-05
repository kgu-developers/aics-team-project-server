package kgu.developers.api.topiccandidate.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import lombok.Builder;

@Builder
public record TopicCandidatePersistResponse(

    @Schema(description = "생성된 주제 후보 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "제안자 학번", example = "202412345", requiredMode = REQUIRED)
    String proposerUserId,

    @Schema(description = "주제 제목", example = "AI 기반 학습 도우미", requiredMode = REQUIRED)
    String title,

    @Schema(description = "주제 설명", example = "학생별 맞춤형 학습 계획을 지원하는 서비스", requiredMode = REQUIRED)
    String description
) {

    public static TopicCandidatePersistResponse of(TopicCandidate topicCandidate) {
        return TopicCandidatePersistResponse.builder()
            .id(topicCandidate.getId())
            .proposerUserId(topicCandidate.getProposerUserId())
            .title(topicCandidate.getTitle())
            .description(topicCandidate.getDescription())
            .build();
    }
}
