package kgu.developers.api.section.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.joining;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;

public record SectionResponse(
    @Schema(description = "분반 ID", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "분반 코드", example = "1154", requiredMode = REQUIRED)
    String code,

    @Schema(description = "요일·시간/분반코드 표시 문자열", example = "월123/1154", requiredMode = REQUIRED)
    String name,

    @Schema(description = "수업시간", example = "월123", requiredMode = REQUIRED)
    String classTime,

    @Schema(description = "정원", example = "40", requiredMode = REQUIRED)
    Integer capacity,

    @Schema(description = "연락처 공개 시작", example = "2026-03-02T00:00:00")
    LocalDateTime contactVisibleFrom,

    @Schema(description = "연락처 공개 종료", example = "2026-06-20T18:00:00")
    LocalDateTime contactVisibleUntil,

    @Schema(description = "강좌 ID", example = "1", requiredMode = REQUIRED)
    Long courseId,

    @Schema(description = "강좌명", example = "객체지향프로그래밍", requiredMode = REQUIRED)
    String courseName,

    @Schema(description = "학년도", example = "2026", requiredMode = REQUIRED)
    Integer year,

    @Schema(
        description = "학기",
        example = "SPRING",
        allowableValues = {"SPRING", "SUMMER", "FALL", "WINTER"},
        requiredMode = REQUIRED
    )
    SemesterType semester,

    @Schema(
        description = "강좌 상태",
        example = "ACTIVE",
        allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"},
        requiredMode = REQUIRED
    )
    StatusType status
) {

    public static SectionResponse from(SectionDetail detail) {
        Section section = detail.section();
        Course course = detail.course();
        return new SectionResponse(
            section.getId(),
            section.getCode(),
            Stream.of(section.getClassTime(), section.getCode())
                .filter(Objects::nonNull)
                .collect(joining("/")),
            section.getClassTime(),
            section.getCapacity(),
            section.getContactVisibleFrom(),
            section.getContactVisibleUntil(),
            course.getId(),
            course.getName(),
            course.getYear(),
            course.getSemester(),
            course.getStatus()
        );
    }
}
