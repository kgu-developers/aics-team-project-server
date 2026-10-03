package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MeetingRecordEditLogAdminPageResponse(

    @Schema(description = "회의록 수정 이력 목록", requiredMode = REQUIRED)
    List<MeetingRecordEditLogAdminResponse> contents,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<MeetingRecordEditLogAdminResponse> pageable
) {
    public static MeetingRecordEditLogAdminPageResponse from(
        Page<MeetingRecordEditLog> page,
        Map<Long, MeetingRecord> meetingRecordsById,
        Map<Long, Team> teamsById,
        Map<String, User> editorsByStudentNumber
    ) {
        List<MeetingRecordEditLogAdminResponse> contents = page.getContent().stream()
            .map(editLog -> MeetingRecordEditLogAdminResponse.from(
                editLog,
                meetingRecordsById.get(editLog.getMeetingRecordId()),
                teamsById.get(editLog.getTeamId()),
                editorsByStudentNumber.get(editLog.getEditorId())))
            .toList();

        PageableResponse<MeetingRecordEditLogAdminResponse> pageable =
            PageableResponse.<MeetingRecordEditLogAdminResponse>builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .isEnd(page.isLast())
                .build();

        return MeetingRecordEditLogAdminPageResponse.builder()
            .contents(contents)
            .pageable(pageable)
            .build();
    }
}
