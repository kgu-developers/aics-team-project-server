package kgu.developers.admin.section.presentation.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SectionAdminUpdateRequest(
    @Schema(description = "교수 학번", example = "202699999")
    @Pattern(regexp = ".*\\S.*", message = "값을 보냈다면 공백일 수 없습니다.")
    String professorId,

    @Schema(description = "강좌 ID", example = "1")
    @Positive
    Long courseId,

    @Schema(description = "과목 코드", example = "1154")
    @Pattern(regexp = ".*\\S.*", message = "값을 보냈다면 공백일 수 없습니다.")
    String code,

    @Schema(description = "분반명", example = "월123/1154")
    @Pattern(regexp = ".*\\S.*", message = "값을 보냈다면 공백일 수 없습니다.")
    String name,

    @Schema(description = "수업시간", example = "월123")
    @Pattern(regexp = ".*\\S.*", message = "값을 보냈다면 공백일 수 없습니다.")
    String classTime,

    @Schema(description = "정원", example = "40")
    @Positive
    Integer capacity
) {
}
