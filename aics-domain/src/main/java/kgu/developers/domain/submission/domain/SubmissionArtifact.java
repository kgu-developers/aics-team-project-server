package kgu.developers.domain.submission.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionArtifact {
    private Long id;
    private Long versionId;
    private Long requiredArtifactId;
    private ArtifactType type;
    private Long fileId;
    private String url;
    private String content;

    public static SubmissionArtifact file(Long versionId, Long requiredArtifactId, Long fileId) {
        return SubmissionArtifact.builder()
                .versionId(versionId)
                .requiredArtifactId(requiredArtifactId)
                .type(ArtifactType.FILE)
                .fileId(fileId)
                .build();
    }

    public static SubmissionArtifact link(Long versionId, Long requiredArtifactId, String url) {
        return SubmissionArtifact.builder()
                .versionId(versionId)
                .requiredArtifactId(requiredArtifactId)
                .type(ArtifactType.LINK)
                .url(url)
                .build();
    }

    public static SubmissionArtifact text(Long versionId, Long requiredArtifactId, String content) {
        return SubmissionArtifact.builder()
                .versionId(versionId)
                .requiredArtifactId(requiredArtifactId)
                .type(ArtifactType.TEXT)
                .content(content)
                .build();
    }

    // 브라우저에서 CheerpJ로 실행 가능한 JAR을 가리키는 URL. LINK와 저장 형태(url 컬럼)는
    // 같지만 타입은 CHEERPJ_RUN으로 구분해서 유지한다.
    public static SubmissionArtifact cheerpjRun(Long versionId, Long requiredArtifactId, String url) {
        return SubmissionArtifact.builder()
                .versionId(versionId)
                .requiredArtifactId(requiredArtifactId)
                .type(ArtifactType.CHEERPJ_RUN)
                .url(url)
                .build();
    }
}
