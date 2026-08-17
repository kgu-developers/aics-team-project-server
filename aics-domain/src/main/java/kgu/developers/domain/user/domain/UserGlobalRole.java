package kgu.developers.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserGlobalRole {
    ADMIN("관리자"),
    USER("일반 유저");

    private final String description;
}
