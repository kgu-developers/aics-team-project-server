package kgu.developers.api.sectionannouncement.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record SectionAnnouncementUpdateRequest(

    @Schema(description = "제목", example = "수정된 제목")
    @Size(max = 192)
    String title,

    @Schema(description = "내용", example = "수정된 내용")
    String content,

    @Schema(description = "게시일시", example = "2026-08-25T10:00:00")
    LocalDateTime publishedAt,

    @Schema(description = "삭제할 첨부파일 식별자 목록(응답의 attachments[].id 값)", example = "[3, 4]")
    List<Long> deleteAttachmentIds
) {
}
