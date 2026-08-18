package kgu.developers.domain.team.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    FORMING("형성중"),
    CONFIRMED("확립");

    private final String description;
}
