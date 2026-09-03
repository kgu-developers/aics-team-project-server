package kgu.developers.auth.api.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.auth.domain.LoginRole;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record LoginResponse(
        @Schema(description = "액세스 토큰", requiredMode = REQUIRED)
        String accessToken,

        @Schema(description = "리프레시 토큰", requiredMode = REQUIRED)
        String refreshToken,

        @Schema(description = "사용자 역할", requiredMode = REQUIRED)
        LoginRole role
) {
    public static LoginResponse of(String accessToken, String refreshToken, LoginRole role) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(role)
                .build();
    }
}
