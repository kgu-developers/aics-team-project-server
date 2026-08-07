package kgu.developers.admin.user.presentation.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record UserAdminPersistResponse(
        @Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
        String student_number
) {
    public static UserAdminPersistResponse of(String student_number) {
        return UserAdminPersistResponse.builder()
                .student_number(student_number)
                .build();
    }
}
