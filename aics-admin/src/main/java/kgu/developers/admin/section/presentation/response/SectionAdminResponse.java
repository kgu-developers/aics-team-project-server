package kgu.developers.admin.section.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

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

    @Schema(description = "과목 코드", example = "CS101", requiredMode = REQUIRED)
    String code,

    @Schema(description = "분반명", example = "01", requiredMode = REQUIRED)
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
            section.getName(),
            section.getClassTime(),
            section.getCapacity(),
            section.getContactVisibleFrom(),
            section.getContactVisibleUntil()
        );
    }
}
