package kgu.developers.admin.course.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record CourseRequest(
	@Schema(description = "과목명", example = "객체지향프로그래밍", requiredMode = REQUIRED)
	@NotNull
	String name,

	@Schema(description = "학년도", example = "2026", requiredMode = REQUIRED)
	@NotNull
	Integer year,

	@Schema(description = "학기", example = "FALL", requiredMode = REQUIRED)
	@NotNull
	SemesterType semester,

	@Schema(description = "상태", example = "DRAFT", requiredMode = REQUIRED)
	@NotNull
	StatusType status
) {
}
