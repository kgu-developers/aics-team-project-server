package kgu.developers.api.sectionannouncement.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import lombok.Builder;

@Builder
public record SectionAnnouncementResponse(

    @Schema(description = "공지사항 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "분반 식별자", example = "1", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "제목", example = "중간고사 일정 안내", requiredMode = REQUIRED)
    String title,

    @Schema(description = "내용", example = "다음 주 화요일 수업시간에 진행합니다.", requiredMode = REQUIRED)
    String content,

    @Schema(description = "게시일시", example = "2026-08-24 10:00", requiredMode = REQUIRED)
    String publishedAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static SectionAnnouncementResponse from(SectionAnnouncement announcement) {
        return SectionAnnouncementResponse.builder()
            .id(announcement.getId())
            .sectionId(announcement.getSectionId())
            .title(announcement.getTitle())
            .content(announcement.getContent())
            .publishedAt(announcement.getPublishedAt().format(FORMATTER))
            .build();
    }
}
