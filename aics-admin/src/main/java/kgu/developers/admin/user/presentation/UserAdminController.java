package kgu.developers.admin.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.admin.user.presentation.response.UserAdminPersistResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저 API")
public interface UserAdminController {

	@Operation(summary = "유저 생성 API", description = """
			- Description : 이 API는 신규 유저를 생성합니다.
		""")
	@ApiResponse(
		responseCode = "201",
		content = @Content(schema = @Schema(implementation = UserAdminPersistResponse.class)))
	ResponseEntity<UserAdminPersistResponse> createUser(
		@Parameter(
			description = "유저 생성 request 객체 입니다.",
			required = true
		) @Valid @RequestBody UserAdminRequest request
	);

	@Operation(summary = "유저 단건 조회 API", description = """
			- Description : 이 API는 학번으로 유저 한 명을 조회합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = UserAdminResponse.class)))
	ResponseEntity<UserAdminResponse> getUserByStudentNumber(
		@Parameter(
			description = "학번은 URL 경로 변수 입니다.",
			example = "202699999",
			required = true
		) @NotBlank @PathVariable String studentNumber
	);

	@Operation(summary = "유저 목록 조회 API", description = """
			- Description : 이 API는 모든 유저 목록을 조회합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = UserAdminListResponse.class)))
	ResponseEntity<UserAdminListResponse> getUsers();

	@Operation(summary = "유저 수정 API", description = """
			- Description : 이 API는 기존 유저 정보를 수정합니다.
		""")
	@ApiResponse(responseCode = "204")
	ResponseEntity<Void> updateUser(
		@Parameter(
			description = "학번은 URL 경로 변수 입니다.",
			example = "202699999",
			required = true
		) @NotBlank @PathVariable String studentNumber,
		@Parameter(
			description = "유저 수정 request 객체 입니다.",
			required = true
		) @Valid @RequestBody UserAdminRequest request
	);

	@Operation(summary = "유저 삭제 API", description = """
			- Description : 이 API는 지정된 유저를 삭제합니다.
		""")
	@ApiResponse(responseCode = "204")
	ResponseEntity<Void> deleteUser(
		@Parameter(
			description = "학번은 URL 경로 변수 입니다.",
			example = "202699999",
			required = true
		) @NotBlank @PathVariable String studentNumber
	);
}
