package kgu.developers.domain.feedback.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequiredArtifactType {
    FILE("파일"),
    LINK("링크"),
    TEXT("텍스트"),
    CHEERPJ_RUN("CheerpJ 실행");

    private final String description;
}
