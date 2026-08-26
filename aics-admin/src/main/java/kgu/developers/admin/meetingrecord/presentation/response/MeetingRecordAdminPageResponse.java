package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Team;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MeetingRecordAdminPageResponse(

    @Schema(description = "회의록 목록", requiredMode = REQUIRED)
    List<MeetingRecordAdminResponse> contents,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<MeetingRecordAdminResponse> pageable
) {
    public static MeetingRecordAdminPageResponse from(
        Page<MeetingRecord> page,
        Map<Long, Team> teamsById,
        Map<Long, Section> sectionsById
    ) {
        List<MeetingRecordAdminResponse> contents = page.getContent().stream()
            .map(meetingRecord -> {
                Team team = teamsById.get(meetingRecord.getTeamId());
                Section section = sectionsById.get(team.getSectionId());
                return MeetingRecordAdminResponse.from(meetingRecord, team, section);
            })
            .toList();

        PageableResponse<MeetingRecordAdminResponse> pageable = PageableResponse.<MeetingRecordAdminResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .isEnd(page.isLast())
            .build();

        return MeetingRecordAdminPageResponse.builder()
            .contents(contents)
            .pageable(pageable)
            .build();
    }
}
