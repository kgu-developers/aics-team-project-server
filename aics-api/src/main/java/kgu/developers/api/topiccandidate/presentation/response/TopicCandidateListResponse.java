package kgu.developers.api.topiccandidate.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicVote.domain.TopicVote;
import lombok.Builder;

@Builder
public record TopicCandidateListResponse(

    @Schema(description = "주제 후보 목록", requiredMode = REQUIRED)
    List<TopicCandidateResponse> contents
) {

    public static TopicCandidateListResponse of(List<TopicCandidate> candidates, List<TopicVote> votes, String userId) {
        Map<Long, Long> voteCounts = votes.stream()
            .collect(java.util.stream.Collectors.groupingBy(TopicVote::getCandidateId, java.util.stream.Collectors.counting()));
        Set<Long> votedCandidateIds = votes.stream()
            .filter(vote -> vote.getVoterUserId().equals(userId))
            .map(TopicVote::getCandidateId)
            .collect(java.util.stream.Collectors.toSet());

        return TopicCandidateListResponse.builder()
            .contents(candidates.stream()
                .map(candidate -> TopicCandidateResponse.of(
                    candidate,
                    voteCounts.getOrDefault(candidate.getId(), 0L),
                    votedCandidateIds.contains(candidate.getId())
                ))
                .toList())
            .build();
    }

    @Builder
    public record TopicCandidateResponse(

        @Schema(description = "주제 후보 식별자", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "주제 제목", example = "AI 기반 학습 도우미", requiredMode = REQUIRED)
        String title,

        @Schema(description = "제안자 학번", example = "202412345", requiredMode = REQUIRED)
        String proposerUserId,

        @Schema(description = "주제 설명", example = "학생별 맞춤형 학습 계획을 지원하는 서비스", requiredMode = REQUIRED)
        String description,

        @Schema(description = "득표 수", example = "3", requiredMode = REQUIRED)
        long voteCount,

        @Schema(description = "현재 사용자의 투표 여부", example = "true", requiredMode = REQUIRED)
        boolean votedByMe
    ) {

        private static TopicCandidateResponse of(TopicCandidate candidate, long voteCount, boolean votedByMe) {
            return TopicCandidateResponse.builder()
                .id(candidate.getId())
                .title(candidate.getTitle())
                .proposerUserId(candidate.getProposerUserId())
                .description(candidate.getDescription())
                .voteCount(voteCount)
                .votedByMe(votedByMe)
                .build();
        }
    }
}
