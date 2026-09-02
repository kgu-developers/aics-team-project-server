package kgu.developers.domain.project.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "project", uniqueConstraints = @UniqueConstraint(name = "uk_project_team", columnNames = "team_id"))
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProjectJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_team_project"))
    private TeamJpaEntity team;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    @Column(length = 255)
    private String repositoryUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode externalLinks;

    @Enumerated(STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus;

    @Column(length = 200)
    private String meetingStyle;

    @Column(name = "proposal_completed_at")
    private LocalDateTime proposalCompletedAt;

    @Column(name = "proposal_revision", nullable = false)
    private long proposalRevision;

    @Version
    private long version;

    public Project toDomain() {
        return Project.builder()
                .id(id)
                .teamId(team.getId())
                .title(title)
                .description(description)
                .goal(goal)
                .repositoryUrl(repositoryUrl)
                .externalLinks(externalLinks)
                .approvalStatus(approvalStatus)
                .meetingStyle(meetingStyle)
                .proposalCompletedAt(proposalCompletedAt)
                .proposalRevision(proposalRevision)
                .version(version)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }

    public static ProjectJpaEntity toEntity(Project project, TeamJpaEntity team) {
        ProjectJpaEntity entity = ProjectJpaEntity.builder()
                .id(project.getId())
                .team(team)
                .title(project.getTitle())
                .description(project.getDescription())
                .goal(project.getGoal())
                .repositoryUrl(project.getRepositoryUrl())
                .externalLinks(project.getExternalLinks())
                .approvalStatus(project.getApprovalStatus())
                .meetingStyle(project.getMeetingStyle())
                .proposalCompletedAt(project.getProposalCompletedAt())
                .proposalRevision(project.getProposalRevision())
                .version(project.getVersion())
                .build();
        entity.createdAt = project.getCreatedAt();
        entity.setDeletedAt(project.getDeletedAt());
        return entity;
    }
}
