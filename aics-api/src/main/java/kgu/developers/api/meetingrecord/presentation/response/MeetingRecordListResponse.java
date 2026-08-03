package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import lombok.Builder;

@Builder
public record MeetingRecordListResponse(

    @Schema(description = "회의록 목록", requiredMode = REQUIRED)
    List<MeetingRecordSummaryResponse> contents
) {

    public static MeetingRecordListResponse from(List<MeetingRecord> meetingRecords) {
        return MeetingRecordListResponse.builder()
            .contents(meetingRecords.stream().map(MeetingRecordSummaryResponse::from).toList())
            .build();
    }
}
