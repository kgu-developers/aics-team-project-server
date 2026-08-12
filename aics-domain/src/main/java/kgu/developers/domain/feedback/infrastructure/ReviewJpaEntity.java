package kgu.developers.domain.feedback.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.feedback.domain.Review;
import kgu.developers.domain.feedback.domain.ReviewResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "review",
        indexes = {
                @Index(name = "idx_review_version", columnList = "version_id"),
                @Index(name = "idx_review_reviewer", columnList = "reviewer_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ReviewJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "reviewer_id", nullable = false, length = 16)
    private String reviewerId;

    @Column(name = "result_status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private ReviewResultStatus resultStatus;

    @Column(length = 2000)
    private String comment;

    public Review toDomain() {
        return Review.restore(
                id,
                versionId,
                reviewerId,
                resultStatus,
                comment,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }

    public static ReviewJpaEntity toEntity(Review review) {
        ReviewJpaEntity entity = ReviewJpaEntity.builder()
                .id(review.getId())
                .versionId(review.getVersionId())
                .reviewerId(review.getReviewerId())
                .resultStatus(review.getResultStatus())
                .comment(review.getComment())
                .build();
        entity.createdAt = review.getCreatedAt();
        entity.updatedAt = review.getUpdatedAt();
        entity.setDeletedAt(review.getDeletedAt());
        return entity;
    }
}
