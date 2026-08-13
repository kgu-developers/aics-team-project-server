package kgu.developers.domain.teamMember.domain;

import lombok.*;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamMember {
    private Long id;
    private Long teamId;
    private String userId;
    private boolean isLeader;
    private String projectRole;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static TeamMember create(Long teamId, String userId, boolean isLeader, String projectRole) {
        return TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .isLeader(isLeader)
                .projectRole(projectRole)
                .build();
    }

    public void updateIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }

    public void updateProjectRole(String projectRole) {
        this.projectRole = projectRole;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
