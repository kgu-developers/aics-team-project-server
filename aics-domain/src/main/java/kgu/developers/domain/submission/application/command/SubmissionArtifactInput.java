package kgu.developers.domain.submission.application.command;

import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.submission.domain.ArtifactType;

// FILE이면 file만, LINK면 url만, TEXT/CHEERPJ_RUN이면 content만 채워서 넘기면 된다.
public record SubmissionArtifactInput(
        Long requiredArtifactId,
        ArtifactType type,
        MultipartFile file,
        String url,
        String content
) {
}
