package kgu.developers.api.editlock.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.editlock.domain.EditLock;
import lombok.Builder;

@Builder
public record EditLockStatusResponse(

    @Schema(description = "잠겨있는지 여부", example = "true", requiredMode = REQUIRED)
    boolean locked,

    @Schema(description = "잠근 사용자 학번(잠긴 경우만)", example = "202412345")
    String lockedBy,

    @Schema(description = "잠근 시각(잠긴 경우만)", example = "2026-08-24 15:00")
    String lockedAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static EditLockStatusResponse unlocked() {
        return EditLockStatusResponse.builder().locked(false).build();
    }

    public static EditLockStatusResponse from(EditLock editLock) {
        return EditLockStatusResponse.builder()
            .locked(true)
            .lockedBy(editLock.getLockedBy())
            .lockedAt(editLock.getLockedAt().format(FORMATTER))
            .build();
    }
}
