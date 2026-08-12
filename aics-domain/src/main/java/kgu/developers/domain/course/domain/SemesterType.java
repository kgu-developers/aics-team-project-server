package kgu.developers.domain.course.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SemesterType {
    SPRING("봄"),
    SUMMER("여름"),
    FALL("가을"),
    WINTER("겨울");

    private final String description;
}
