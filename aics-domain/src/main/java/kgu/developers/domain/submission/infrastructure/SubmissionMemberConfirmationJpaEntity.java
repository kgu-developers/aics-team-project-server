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

import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "submission_member_confirmation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_member_confirmation",
                columnNames = { "submission_id", "user_id" })
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionMemberConfirmationJpaEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "confirmed_final_report", nullable = false)
    private boolean confirmedFinalReport;

    @Column(name = "confirmed_artifacts", nullable = false)
    private boolean confirmedArtifacts;

    @Column(name = "one_line_review", columnDefinition = "text")
    private String oneLineReview;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    public static SubmissionMemberConfirmationJpaEntity fromDomain(SubmissionMemberConfirmation confirmation) {
        return SubmissionMemberConfirmationJpaEntity.builder()
                .id(confirmation.getId())
                .submissionId(confirmation.getSubmissionId())
                .userId(confirmation.getUserId())
                .version(confirmation.getVersion())
                .confirmedFinalReport(confirmation.isConfirmedFinalReport())
                .confirmedArtifacts(confirmation.isConfirmedArtifacts())
                .oneLineReview(confirmation.getOneLineReview())
                .confirmedAt(confirmation.getConfirmedAt())
                .build();
    }

    public SubmissionMemberConfirmation toDomain() {
        return SubmissionMemberConfirmation.builder()
                .id(id)
                .submissionId(submissionId)
                .userId(userId)
                .version(version)
                .confirmedFinalReport(confirmedFinalReport)
                .confirmedArtifacts(confirmedArtifacts)
                .oneLineReview(oneLineReview)
                .confirmedAt(confirmedAt)
                .build();
    }
}
