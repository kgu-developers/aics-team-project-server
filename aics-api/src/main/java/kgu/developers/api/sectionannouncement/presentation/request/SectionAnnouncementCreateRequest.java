package kgu.developers.api.sectionannouncement.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SectionAnnouncementCreateRequest(

    // 200자 제한(Notification.title 컬럼)에서 브로드캐스트 접두어("새 공지사항: ", 7자)를 뺀 값.
    @Schema(description = "제목", example = "중간고사 일정 안내", requiredMode = REQUIRED)
    @NotBlank
    @Size(max = 193)
    String title,

    @Schema(description = "내용", example = "다음 주 화요일 수업시간에 진행합니다.", requiredMode = REQUIRED)
    @NotBlank
    String content,

    @Schema(description = "게시일시(미지정 시 현재 시각)", example = "2026-08-24T10:00:00")
    LocalDateTime publishedAt
) {
}
