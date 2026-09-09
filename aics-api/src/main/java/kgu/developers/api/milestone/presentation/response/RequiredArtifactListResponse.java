package kgu.developers.api.milestone.presentation.response;

import java.util.List;

import kgu.developers.domain.feedback.domain.RequiredArtifact;

public record RequiredArtifactListResponse(List<RequiredArtifactResponse> contents) {
    public static RequiredArtifactListResponse from(List<RequiredArtifact> requiredArtifacts) {
        return new RequiredArtifactListResponse(
                requiredArtifacts.stream().map(RequiredArtifactResponse::from).toList()
        );
    }
}
