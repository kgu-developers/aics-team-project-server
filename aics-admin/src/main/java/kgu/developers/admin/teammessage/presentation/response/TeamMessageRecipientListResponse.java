package kgu.developers.admin.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record TeamMessageRecipientListResponse(
    @Schema(description = "수신 팀 목록", requiredMode = REQUIRED)
    List<TeamMessageRecipientResponse> contents
) {
    public static TeamMessageRecipientListResponse from(List<TeamMessageRecipientResponse> contents) {
        return TeamMessageRecipientListResponse.builder()
            .contents(contents)
            .build();
    }
}
