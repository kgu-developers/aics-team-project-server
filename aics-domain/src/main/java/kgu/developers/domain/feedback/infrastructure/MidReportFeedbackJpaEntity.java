package kgu.developers.domain.feedback.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.feedback.domain.MidReportFeedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "mid_report_feedback",
        indexes = {
                @Index(name = "idx_mid_report_feedback_submission", columnList = "submission_id"),
                @Index(name = "idx_mid_report_feedback_author", columnList = "author_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MidReportFeedbackJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "author_id", nullable = false, length = 16)
    private String authorId;

    @Column(name = "onsite_feedback_summary", nullable = false, length = 2000)
    private String onsiteFeedbackSummary;

    @Column(name = "professor_additional_feedback", length = 2000)
    private String professorAdditionalFeedback;

    @Column(name = "revision_note", length = 2000)
    private String revisionNote;

    public MidReportFeedback toDomain() {
        return MidReportFeedback.restore(
                id,
                submissionId,
                authorId,
                onsiteFeedbackSummary,
                professorAdditionalFeedback,
                revisionNote,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }

    public static MidReportFeedbackJpaEntity toEntity(MidReportFeedback feedback) {
        MidReportFeedbackJpaEntity entity = MidReportFeedbackJpaEntity.builder()
                .id(feedback.getId())
                .submissionId(feedback.getSubmissionId())
                .authorId(feedback.getAuthorId())
                .onsiteFeedbackSummary(feedback.getOnsiteFeedbackSummary())
                .professorAdditionalFeedback(feedback.getProfessorAdditionalFeedback())
                .revisionNote(feedback.getRevisionNote())
                .build();
        entity.createdAt = feedback.getCreatedAt();
        entity.updatedAt = feedback.getUpdatedAt();
        entity.setDeletedAt(feedback.getDeletedAt());
        return entity;
    }
}
