package kgu.developers.admin.user.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kgu.developers.common.validation.BcryptPassword;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record UserAdminRequest(
        @Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
        @NotBlank
        @Size(max = 16)
        String studentNumber,

        @Schema(description = "이메일", example = "kgu@kyonggi.ac.kr", requiredMode = REQUIRED)
        @NotBlank
        @Email
        @Size(max = 64)
        String email,

        @Schema(description = "이름", example = "김철수", requiredMode = REQUIRED)
        @NotBlank
        @Size(max = 32)
        String name,

        @Schema(description = "비밀번호", example = "12345678", requiredMode = REQUIRED)
        @NotBlank
        @Size(min = 4, max = 64)
        @BcryptPassword
        String password,

        @Schema(description = "권한", example = "USER", requiredMode = REQUIRED)
        @NotNull
        UserGlobalRole globalRole,

        @Schema(description = "전화번호", example = "010-1234-6789", requiredMode = REQUIRED)
        @NotBlank
        @Size(max = 20)
        String phone,

        @Schema(description = "탈퇴한 학번을 재사용해 새 계정을 만들지 여부. 기존 계정은 이력으로 보관된 뒤 삭제된다.",
                example = "false")
        boolean reactivate
) {

}
