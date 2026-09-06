package kgu.developers.admin.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.SubmissionArtifact;

@Builder
public record SubmissionArtifactAdminResponse(

        @Schema(description = "요구 산출물 식별자", example = "1")
        Long requiredArtifactId,

        @Schema(description = "아티팩트 종류", example = "FILE", requiredMode = REQUIRED)
        ArtifactType type,

        @Schema(description = "파일 식별자(FILE일 때만)", example = "42")
        Long fileId,

        @Schema(description = "파일 이름(FILE일 때만)", example = "발표자료.pdf")
        String fileName,

        @Schema(description = "다운로드용 임시 URL(FILE일 때만, 15분 후 만료)")
        String downloadUrl,

        @Schema(description = "URL(LINK일 때만)", example = "https://github.com/kgu-developers/aics-team-project-server")
        String url,

        @Schema(description = "본문(TEXT일 때만)")
        String content
) {

    public static SubmissionArtifactAdminResponse ofFile(SubmissionArtifact artifact, FileObject fileObject, String downloadUrl) {
        return SubmissionArtifactAdminResponse.builder()
                .requiredArtifactId(artifact.getRequiredArtifactId())
                .type(artifact.getType())
                .fileId(artifact.getFileId())
                .fileName(fileObject.getFileName())
                .downloadUrl(downloadUrl)
                .build();
    }

    public static SubmissionArtifactAdminResponse of(SubmissionArtifact artifact) {
        return SubmissionArtifactAdminResponse.builder()
                .requiredArtifactId(artifact.getRequiredArtifactId())
                .type(artifact.getType())
                .url(artifact.getUrl())
                .content(artifact.getContent())
                .build();
    }
}
