package kgu.developers.api.topicvote.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.topicVote.domain.TopicVote;
import lombok.Builder;

@Builder
public record TopicVotePersistResponse(

    @Schema(description = "투표 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "주제 후보 식별자", example = "1", requiredMode = REQUIRED)
    Long candidateId,

    @Schema(description = "투표자 학번", example = "202412345", requiredMode = REQUIRED)
    String voterUserId
) {

    public static TopicVotePersistResponse of(TopicVote topicVote) {
        return TopicVotePersistResponse.builder()
            .id(topicVote.getId())
            .candidateId(topicVote.getCandidateId())
            .voterUserId(topicVote.getVoterUserId())
            .build();
    }
}
