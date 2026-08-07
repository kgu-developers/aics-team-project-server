package kgu.developers.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGlobalRole {
    PROFESSOR("교수"),
    STUDENT("학생");

    private final String description;
}
