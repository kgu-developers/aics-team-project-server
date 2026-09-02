package kgu.developers.domain.submission.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.submission.domain.RevisionProgressStatus;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "submission",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_team_milestone", columnNames = { "team_id", "milestone_id" })
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "revision_due_at")
    private LocalDateTime revisionDueAt;

    @Enumerated(STRING)
    @Column(name = "revision_progress", length = 16)
    private RevisionProgressStatus revisionProgress;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopened_by", length = 20)
    private String reopenedBy;

    @Column(name = "presentation_order")
    private Integer presentationOrder;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 20)
    private String completedBy;

    public static SubmissionJpaEntity fromDomain(Submission submission) {
        return SubmissionJpaEntity.builder()
                .id(submission.getId())
                .teamId(submission.getTeamId())
                .milestoneId(submission.getMilestoneId())
                .status(submission.getStatus())
                .currentVersion(submission.getCurrentVersion())
                .revisionDueAt(submission.getRevisionDueAt())
                .revisionProgress(submission.getRevisionProgress())
                .reopenedAt(submission.getReopenedAt())
                .reopenedBy(submission.getReopenedBy())
                .presentationOrder(submission.getPresentationOrder())
                .completedAt(submission.getCompletedAt())
                .completedBy(submission.getCompletedBy())
                .build();
    }

    public Submission toDomain() {
        return Submission.builder()
                .id(id)
                .teamId(teamId)
                .milestoneId(milestoneId)
                .status(status)
                .currentVersion(currentVersion)
                .revisionDueAt(revisionDueAt)
                .revisionProgress(revisionProgress)
                .reopenedAt(reopenedAt)
                .reopenedBy(reopenedBy)
                .presentationOrder(presentationOrder)
                .completedAt(completedAt)
                .completedBy(completedBy)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }
}
