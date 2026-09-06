package kgu.developers.api.meetingrecord.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import lombok.Builder;

@Builder
public record MeetingRecordUpdateRequest(

    @Schema(description = "회의록 제목", example = "3주차 정기 회의(수정)")
    String title,

    @Schema(description = "회의 일시", example = "2026-08-03T15:00:00")
    LocalDateTime meetingAt,

    @Schema(description = "장소/진행방식", example = "온라인(Zoom)")
    String location,

    @Schema(description = "회의 단계(PROPOSAL:제안, MID_CHECK:중간, FINAL:최종)", example = "FINAL")
    MeetingPhase phase,

    @Schema(description = "회의 내용", example = "수정된 회의 내용")
    String content,

    @Schema(description = "참석자 학번 목록(주어지면 기존 참석자 전체를 치환)", example = "[\"202412345\", \"202412347\"]")
    List<String> participantIds
) {
}
