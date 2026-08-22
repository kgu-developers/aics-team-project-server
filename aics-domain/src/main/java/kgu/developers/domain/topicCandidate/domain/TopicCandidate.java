package kgu.developers.domain.topicCandidate.domain;

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
public class TopicCandidate {
    private Long id;

    private Long teamId;  // 팀 식별자
    private String proposerUserId;  // 제안자 학번

    private String title;  // 제목
    private String description;  // 설명

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static TopicCandidate create(Long teamId, String proposerUserId, String title, String description) {
        return TopicCandidate.builder()
                .teamId(requireNonNull(teamId, "teamId"))
                .proposerUserId(requireNonNull(proposerUserId, "proposerUserId"))
                .title(requireNonNull(title, "title"))
                .description(requireNonNull(description, "description"))
                .build();
    }

    public void updateTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
