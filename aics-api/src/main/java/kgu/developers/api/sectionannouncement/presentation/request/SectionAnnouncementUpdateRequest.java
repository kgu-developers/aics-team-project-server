package kgu.developers.api.sectionannouncement.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SectionAnnouncementUpdateRequest(

    @Schema(description = "제목", example = "수정된 제목")
    @Size(max = 193)
    String title,

    @Schema(description = "내용", example = "수정된 내용")
    String content,

    @Schema(description = "게시일시", example = "2026-08-25T10:00:00")
    LocalDateTime publishedAt
) {
}
