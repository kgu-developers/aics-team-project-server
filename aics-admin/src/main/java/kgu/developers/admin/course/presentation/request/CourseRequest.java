package kgu.developers.admin.course.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record CourseRequest(
	@Schema(description = "과목명", example = "객체지향프로그래밍", requiredMode = REQUIRED)
	@NotBlank
	@Size(max = 64)
	String name,

	@Schema(description = "학년도", example = "2026", requiredMode = REQUIRED)
	@NotNull
	@Min(2000)
	@Max(2100)
	Integer year,

	@Schema(description = "학기", example = "FALL", requiredMode = REQUIRED)
	@NotNull
	SemesterType semester,

	@Schema(description = "상태", example = "DRAFT", requiredMode = REQUIRED)
	@NotNull
	StatusType status
) {
}
