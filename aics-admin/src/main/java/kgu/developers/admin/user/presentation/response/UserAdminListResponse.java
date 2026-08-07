package kgu.developers.admin.user.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record UserAdminListResponse(
        @Schema(description = "유저 리스트", requiredMode = REQUIRED)
        List<UserAdminResponse> contents
) {
    public static UserAdminListResponse from(List<User> users) {
        return UserAdminListResponse.builder()
                .contents(users.stream().map(UserAdminResponse::from).toList())
                .build();
    }
}
