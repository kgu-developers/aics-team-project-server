package kgu.developers.api.topiccandidate.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;

public record TopicFinalizeResponse(

    @Schema(description = "생성 또는 갱신된 프로젝트 식별자", example = "1")
    Long projectId,

    @Schema(description = "확정된 주제 후보 식별자", example = "1")
    Long candidateId,

    @Schema(description = "확정된 주제 제목", example = "AI 기반 학습 도우미")
    String title
) {
    public static TopicFinalizeResponse of(Project project, TopicCandidate topicCandidate) {
        return new TopicFinalizeResponse(project.getId(), topicCandidate.getId(), project.getTitle());
    }
}
