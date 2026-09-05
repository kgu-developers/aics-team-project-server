package kgu.developers.admin.section.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.joining;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.admin.course.presentation.response.CourseResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;

public record SectionAdminResponse(
    @Schema(description = "분반 ID", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(requiredMode = REQUIRED)
    UserAdminResponse professor,

    @Schema(requiredMode = REQUIRED)
    CourseResponse course,

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
    LocalDateTime contactVisibleUntil
) {

    public static SectionAdminResponse from(SectionDetail detail) {
        Section section = detail.section();
        return new SectionAdminResponse(
            section.getId(),
            UserAdminResponse.from(detail.professor()),
            CourseResponse.from(detail.course()),
            section.getCode(),
            Stream.of(section.getClassTime(), section.getCode())
                .filter(Objects::nonNull)
                .collect(joining("/")),
            section.getClassTime(),
            section.getCapacity(),
            section.getContactVisibleFrom(),
            section.getContactVisibleUntil()
        );
    }
}
