package kgu.developers.auth.api.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.auth.api.presentation.response.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저 API")
public interface AuthController {

	@Operation(summary = "로그인 API", description = """
			- Description : 이 API는 학번과 비밀번호로 로그인합니다.
			- accessToken, refreshToken은 응답 본문이 아니라 HttpOnly 쿠키로 내려갑니다.
		""")
	@ApiResponse(
		responseCode = "200",
		description = "본문은 {\"message\": \"Login successfully\", \"role\": \"STUDENT|ADMIN|ASSISTANT\"}, 토큰은 Set-Cookie(HttpOnly)로 발급")
	@ApiResponse(responseCode = "401", description = "학번 또는 비밀번호가 올바르지 않습니다.")
	ResponseEntity<MessageResponse> login(
		@Parameter(
			description = "로그인 request 객체 입니다.",
			required = true
		) @Valid @RequestBody LoginRequest request
	);

	@Operation(summary = "토큰 재발급 API", description = """
			- Description : 이 API는 refreshToken 쿠키로 accessToken, refreshToken을 재발급합니다.
			- refreshToken은 요청 본문이 아니라 HttpOnly 쿠키에서 읽습니다.
			- 재발급된 토큰도 Set-Cookie로 내려가며, refreshToken도 함께 교체됩니다.
		""")
	@ApiResponse(
		responseCode = "200",
		description = "본문은 {\"message\": \"Refresh successfully\"}, 토큰은 Set-Cookie(HttpOnly)로 재발급")
	@ApiResponse(responseCode = "401", description = "refreshToken이 없거나 유효하지 않습니다.")
	ResponseEntity<MessageResponse> refresh(
		@Parameter(
			description = "refreshToken 쿠키입니다.",
			required = true
		) @CookieValue(name = "refreshToken", required = false) String refreshToken
	);

	@Operation(summary = "로그아웃 API", description = """
			- Description : 이 API는 accessToken, refreshToken 쿠키를 만료시켜 로그아웃합니다.
			- 서버에 저장된 refreshToken도 함께 폐기되어 재발급에 쓸 수 없습니다.
			- 이미 발급된 accessToken은 만료 시각까지 유효합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		description = "본문은 {\"message\": \"Logout successfully\"}, 두 토큰 쿠키를 Max-Age=0으로 만료")
	ResponseEntity<MessageResponse> logout(
		@Parameter(description = "리프레시 토큰 쿠키입니다.", required = false)
		String refreshToken);
}
