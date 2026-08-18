package kgu.developers.domain.importBatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    PREVIEW("미리보기"),
    APPLIED("적용됨"),
    EXPIRED("만료됨");

    private final String description;
}
