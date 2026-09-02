package kgu.developers.domain.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoginRole {
    ADMIN("관리자"),
    STUDENT("학생"),
    ASSISTANT("조교");

    private final String description;
}
