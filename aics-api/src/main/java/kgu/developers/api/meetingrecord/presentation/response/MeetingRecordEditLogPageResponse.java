package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MeetingRecordEditLogPageResponse(

    @Schema(description = "회의록 수정 이력 목록", requiredMode = REQUIRED)
    List<MeetingRecordEditLogResponse> contents,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<MeetingRecordEditLogResponse> pageable
) {
    public static MeetingRecordEditLogPageResponse from(
        Page<MeetingRecordEditLog> page,
        Map<String, User> editorsByStudentNumber
    ) {
        List<MeetingRecordEditLogResponse> contents = page.getContent().stream()
            .map(editLog -> MeetingRecordEditLogResponse.from(
                editLog, editorsByStudentNumber.get(editLog.getEditorId())))
            .toList();

        PageableResponse<MeetingRecordEditLogResponse> pageable =
            PageableResponse.<MeetingRecordEditLogResponse>builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .isEnd(page.isLast())
                .build();

        return MeetingRecordEditLogPageResponse.builder()
            .contents(contents)
            .pageable(pageable)
            .build();
    }
}
