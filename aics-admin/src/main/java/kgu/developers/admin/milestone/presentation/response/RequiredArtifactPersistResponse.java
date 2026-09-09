package kgu.developers.admin.milestone.presentation.response;

public record RequiredArtifactPersistResponse(Long id) {
    public static RequiredArtifactPersistResponse of(Long id) {
        return new RequiredArtifactPersistResponse(id);
    }
}
