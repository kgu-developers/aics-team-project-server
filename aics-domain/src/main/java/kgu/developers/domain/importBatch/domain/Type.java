package kgu.developers.domain.importBatch.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    ENROLLMENT("수강생"),
    TEAM("팀");

    private final String description;
}
