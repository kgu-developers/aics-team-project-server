package kgu.developers.admin.course.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.course.domain.Course;
import lombok.Builder;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record CourseListResponse(
	@Schema(description = "등록된 강좌 리스트", requiredMode = REQUIRED)
	List<Course> contents
) {
	public static CourseListResponse from(List<Course> contents) {
		return CourseListResponse.builder()
			.contents(contents)
			.build();
	}
}
