package kgu.developers.admin.user.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.Builder;

@Builder
public record UserAdminResponse(
	@Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
	String student_number,

	@Schema(description = "이메일", example = "kgu@kyonggi.ac.kr", requiredMode = REQUIRED)
	String email,

	@Schema(description = "이름", example = "김철수", requiredMode = REQUIRED)
	String name,

	@Schema(description = "권한", example = "STUDENT", requiredMode = REQUIRED)
	UserGlobalRole global_role,

	@Schema(description = "전화번호", example = "010-1234-6789", requiredMode = REQUIRED)
	String phone,

	@Schema(description = "생성 일시")
	LocalDateTime created_at,

	@Schema(description = "수정 일시")
	LocalDateTime updated_at
) {
	public static UserAdminResponse from(User user) {
		return UserAdminResponse.builder()
			.student_number(user.getStudent_number())
			.email(user.getEmail())
			.name(user.getName())
			.global_role(user.getGlobal_role())
			.phone(user.getPhone())
			.created_at(user.getCreated_at())
			.updated_at(user.getUpdated_at())
			.build();
	}
}
