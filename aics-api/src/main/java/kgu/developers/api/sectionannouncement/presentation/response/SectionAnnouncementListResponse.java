package kgu.developers.api.sectionannouncement.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import lombok.Builder;

@Builder
public record SectionAnnouncementListResponse(

    @Schema(description = "공지사항 목록", requiredMode = REQUIRED)
    List<SectionAnnouncementResponse> contents
) {

    public static SectionAnnouncementListResponse from(List<SectionAnnouncement> announcements) {
        return SectionAnnouncementListResponse.builder()
            .contents(announcements.stream().map(SectionAnnouncementResponse::from).toList())
            .build();
    }
}
