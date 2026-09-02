package kgu.developers.domain.presentationcontent.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.presentationcontent.domain.PresentationContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "presentation_content",
        uniqueConstraints = @UniqueConstraint(name = "uk_presentation_content_submission", columnNames = "submission_id")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PresentationContentJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "intro_text", columnDefinition = "text")
    private String introText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String features;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String screens;

    @Column(name = "youtube_url", length = 512)
    private String youtubeUrl;

    public static PresentationContentJpaEntity fromDomain(PresentationContent content) {
        return PresentationContentJpaEntity.builder()
                .id(content.getId())
                .submissionId(content.getSubmissionId())
                .introText(content.getIntroText())
                .features(content.getFeatures() == null ? null : content.getFeatures().toString())
                .screens(content.getScreens() == null ? null : content.getScreens().toString())
                .youtubeUrl(content.getYoutubeUrl())
                .build();
    }

    public PresentationContent toDomain() {
        return PresentationContent.builder()
                .id(id)
                .submissionId(submissionId)
                .introText(introText)
                .features(features == null ? null : JsonConverter.parse(features))
                .screens(screens == null ? null : JsonConverter.parse(screens))
                .youtubeUrl(youtubeUrl)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
