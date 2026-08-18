package kgu.developers.api.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UnreadMessageCountResponse(

    @Schema(description = "읽지 않은 메시지 수", example = "3", requiredMode = REQUIRED)
    long count
) {
    public static UnreadMessageCountResponse of(long count) {
        return new UnreadMessageCountResponse(count);
    }
}
