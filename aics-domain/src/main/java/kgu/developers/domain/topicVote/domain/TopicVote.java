package kgu.developers.domain.topicVote.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TopicVote {
    private Long id;

    private Long teamId;  // 팀 식별자. 1인 1표의 범위다.
    private Long candidateId;  // 후보 식별자
    private String voterUserId;  // 투표자 학번

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static TopicVote create(Long teamId, Long candidateId, String voterUserId) {
        return TopicVote.builder()
                .teamId(requireNonNull(teamId, "teamId"))
                .candidateId(requireNonNull(candidateId, "candidateId"))
                .voterUserId(requireNonNull(voterUserId, "voterUserId"))
                .build();
    }

    public void updateCandidateId(Long candidateId) {
        this.candidateId = requireNonNull(candidateId, "candidateId");
    }

    public void updateVoterUserId(String voterUserId) {
        this.voterUserId = requireNonNull(voterUserId, "voterUserId");
    }

    public void reactivate(Long candidateId) {
        this.deletedAt = null;
        this.candidateId = requireNonNull(candidateId, "candidateId");
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
