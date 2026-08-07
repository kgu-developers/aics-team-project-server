package kgu.developers.api.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저 API")
public interface UserController {

	@Operation(summary = "비밀번호 변경 API", description = """
			- Description : 이 API는 유저의 비밀번호를 변경합니다.
		""")
	@ApiResponse(responseCode = "200")
	ResponseEntity<String> updateUserPassword(
		@Parameter(
			description = "학번은 URL 경로 변수 입니다.",
			example = "202699999",
			required = true
		) @NotBlank @PathVariable String studentNumber,
		@Parameter(
			description = "비밀번호 변경 request 객체 입니다.",
			required = true
		) @Valid @RequestBody UserUpdateRequest request
	);
}
