package kgu.developers.domain.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusType {
    DRAFT("임시 저장"),
    ACTIVE("활성"),
    ARCHIVED("보관");

    private final String description;
}
