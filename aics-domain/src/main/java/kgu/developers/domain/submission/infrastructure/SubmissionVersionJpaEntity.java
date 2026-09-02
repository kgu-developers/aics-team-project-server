package kgu.developers.domain.submission.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "submission_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_version", columnNames = { "submission_id", "version" })
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionVersionJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "change_note", columnDefinition = "text")
    private String changeNote;

    @Column(name = "submitted_by", nullable = false, length = 20)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "is_late", nullable = false)
    private boolean late;

    public static SubmissionVersionJpaEntity fromDomain(SubmissionVersion version) {
        return SubmissionVersionJpaEntity.builder()
                .id(version.getId())
                .submissionId(version.getSubmissionId())
                .version(version.getVersion())
                .description(version.getDescription())
                .changeNote(version.getChangeNote())
                .submittedBy(version.getSubmittedBy())
                .submittedAt(version.getSubmittedAt())
                .late(version.isLate())
                .build();
    }

    public SubmissionVersion toDomain() {
        return SubmissionVersion.builder()
                .id(id)
                .submissionId(submissionId)
                .version(version)
                .description(description)
                .changeNote(changeNote)
                .submittedBy(submittedBy)
                .submittedAt(submittedAt)
                .late(late)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }
}
