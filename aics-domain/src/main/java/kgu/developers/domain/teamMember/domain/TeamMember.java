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
    private Long version;  // 낙관적 락 버전 (신규는 null)

    private Long teamId;
    private String userId;
    private boolean isLeader;
    private String projectRole;
    private String phoneNumber;
    private String grade;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static TeamMember create(Long teamId, String userId, boolean isLeader, String projectRole, String phoneNumber, String grade) {
        return TeamMember.builder()
                .teamId(teamId)
                .userId(userId)
                .isLeader(isLeader)
                .projectRole(projectRole)
                .phoneNumber(phoneNumber)
                .grade(grade)
                .build();
    }

    public static TeamMember create(Long teamId, String userId, boolean isLeader, String projectRole) {
        return create(teamId, userId, isLeader, projectRole, null, null);
    }

    public void updateTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public void updateIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }

    public void updateProjectRole(String projectRole) {
        this.projectRole = projectRole;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updateGrade(String grade) {
        this.grade = grade;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate(boolean isLeader, String projectRole, String phoneNumber, String grade) {
        this.deletedAt = null;
        this.isLeader = isLeader;
        this.projectRole = projectRole;
        this.phoneNumber = phoneNumber;
        this.grade = grade;
    }

    public void reactivate(boolean isLeader, String projectRole) {
        reactivate(isLeader, projectRole, null, null);
    }
}
