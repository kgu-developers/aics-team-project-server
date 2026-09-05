package kgu.developers.domain.project.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "project", uniqueConstraints = @UniqueConstraint(name = "uk_project_team", columnNames = {"team_id", "deleted_at"}))
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProjectJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_team_project"))
    private TeamJpaEntity team;

    // 최종 확정된 주제 후보. 다른 애그리게이트라 연관관계 대신 식별자만 들고 있는다.
    @Column(name = "topic_candidate_id")
    private Long topicCandidateId;

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

    public Project toDomain() {
        return Project.builder()
                .id(id)
                .teamId(team.getId())
                .topicCandidateId(topicCandidateId)
                .title(title)
                .description(description)
                .goal(goal)
                .repositoryUrl(repositoryUrl)
                .externalLinks(externalLinks)
                .approvalStatus(approvalStatus)
                .meetingStyle(meetingStyle)
                .proposalCompletedAt(proposalCompletedAt)
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
                .topicCandidateId(project.getTopicCandidateId())
                .title(project.getTitle())
                .description(project.getDescription())
                .goal(project.getGoal())
                .repositoryUrl(project.getRepositoryUrl())
                .externalLinks(project.getExternalLinks())
                .approvalStatus(project.getApprovalStatus())
                .meetingStyle(project.getMeetingStyle())
                .proposalCompletedAt(project.getProposalCompletedAt())
                .version(project.getVersion())
                .build();
        if (project.getId() != null) {
            entity.createdAt = project.getCreatedAt();
        }
        entity.setDeletedAt(project.getDeletedAt());
        return entity;
    }
}
