package kgu.developers.domain.submission.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "submission_artifact")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionArtifactJpaEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "required_artifact_id")
    private Long requiredArtifactId;

    @Enumerated(STRING)
    @Column(nullable = false, length = 16)
    private ArtifactType type;

    @Column(name = "file_id")
    private Long fileId;

    @Column(columnDefinition = "text")
    private String url;

    @Column(columnDefinition = "text")
    private String content;

    public static SubmissionArtifactJpaEntity fromDomain(SubmissionArtifact artifact) {
        return SubmissionArtifactJpaEntity.builder()
                .id(artifact.getId())
                .versionId(artifact.getVersionId())
                .requiredArtifactId(artifact.getRequiredArtifactId())
                .type(artifact.getType())
                .fileId(artifact.getFileId())
                .url(artifact.getUrl())
                .content(artifact.getContent())
                .build();
    }

    public SubmissionArtifact toDomain() {
        return SubmissionArtifact.builder()
                .id(id)
                .versionId(versionId)
                .requiredArtifactId(requiredArtifactId)
                .type(type)
                .fileId(fileId)
                .url(url)
                .content(content)
                .build();
    }
}
