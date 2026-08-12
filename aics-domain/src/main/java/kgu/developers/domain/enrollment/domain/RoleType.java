package kgu.developers.domain.enrollment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    STUDENT("학생"),
    ASSISTANT("조교");

    private final String description;
}
