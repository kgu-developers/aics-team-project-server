package kgu.developers.admin.course.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record CoursePersistResponse(
	@Schema(description = "강좌 ID", example = "1", requiredMode = REQUIRED)
	Long id
) {
	public static CoursePersistResponse of(Long id) {
		return CoursePersistResponse.builder()
			.id(id)
			.build();
	}
}
