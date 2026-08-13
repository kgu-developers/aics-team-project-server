package kgu.developers.domain.teamMember.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "team_member")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamMemberJpaEntity extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private Long id;

  @ManyToOne(fetch = LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_member_team"))
  private TeamJpaEntity team;

  @ManyToOne(fetch = LAZY, optional = false)
  @JoinColumn(name = "user_id", referencedColumnName = "student_number", nullable = false, foreignKey = @ForeignKey(name = "fk_team_member_user"))
  private UserJpaEntity user;

  @Column(nullable = false)
  private boolean isLeader;

  @Column(nullable = false, length = 50)
  private String projectRole;

  public TeamMember toDomain() {
    return TeamMember.builder()
        .id(id)
        .teamId(team.getId())
        .userId(user.getStudentNumber())
        .isLeader(isLeader)
        .projectRole(projectRole)
        .createdAt(getCreatedAt())
        .updatedAt(getUpdatedAt())
        .deletedAt(getDeletedAt())
        .build();
  }

  public static TeamMemberJpaEntity toEntity(TeamMember teamMember, TeamJpaEntity team, UserJpaEntity user) {
    TeamMemberJpaEntity entity = TeamMemberJpaEntity.builder()
        .id(teamMember.getId())
        .team(team)
        .user(user)
        .isLeader(teamMember.isLeader())
        .projectRole(teamMember.getProjectRole())
        .build();
    entity.createdAt = teamMember.getCreatedAt();
    entity.setDeletedAt(teamMember.getDeletedAt());
    return entity;
  }
}
