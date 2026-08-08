package kgu.developers.admin.course.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import lombok.Builder;

@Builder
public record CourseResponse(
	@Schema(description = "강좌 ID", example = "1", requiredMode = REQUIRED)
	Long id,

	@Schema(description = "과목명", example = "객체지향프로그래밍", requiredMode = REQUIRED)
	String name,

	@Schema(description = "학년도", example = "2026", requiredMode = REQUIRED)
	int year,

	@Schema(description = "학기", example = "FALL", requiredMode = REQUIRED)
	SemesterType semester,

	@Schema(description = "상태", example = "DRAFT", requiredMode = REQUIRED)
	StatusType status,

	@Schema(description = "생성 일시")
	LocalDateTime created_at,

	@Schema(description = "수정 일시")
	LocalDateTime updated_at
) {
	public static CourseResponse from(Course course) {
		return CourseResponse.builder()
			.id(course.getId())
			.name(course.getName())
			.year(course.getYear())
			.semester(course.getSemester())
			.status(course.getStatus())
			.created_at(course.getCreatedAt())
			.updated_at(course.getUpdatedAt())
			.build();
	}
}
