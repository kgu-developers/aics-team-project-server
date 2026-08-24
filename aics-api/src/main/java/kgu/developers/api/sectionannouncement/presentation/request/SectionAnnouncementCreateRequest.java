package kgu.developers.api.sectionannouncement.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SectionAnnouncementCreateRequest(

    @Schema(description = "제목", example = "중간고사 일정 안내", requiredMode = REQUIRED)
    @NotBlank
    String title,

    @Schema(description = "내용", example = "다음 주 화요일 수업시간에 진행합니다.", requiredMode = REQUIRED)
    @NotBlank
    String content,

    @Schema(description = "게시일시(미지정 시 현재 시각)", example = "2026-08-24T10:00:00")
    LocalDateTime publishedAt
) {
}
