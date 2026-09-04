package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Team;
import lombok.Builder;

@Builder
public record MeetingRecordAdminResponse(

    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "분반 식별자", example = "10", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "분반명", example = "1151", requiredMode = REQUIRED)
    String sectionName,

    @Schema(description = "팀 식별자", example = "20", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "A팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "회의 단계", example = "MID_CHECK", requiredMode = REQUIRED)
    MeetingPhase phase,

    @Schema(description = "작성자 학번", example = "202412345", requiredMode = REQUIRED)
    String authorId,

    @Schema(description = "회의 일시", example = "2026-08-03 14:00", requiredMode = REQUIRED)
    String meetingAt,

    @Schema(description = "장소/진행방식", example = "온라인(Zoom)")
    String location,

    @Schema(description = "회의 내용", example = "와이어프레임 기획 논의", requiredMode = REQUIRED)
    String content,

    @Schema(description = "참석자 수", example = "4", requiredMode = REQUIRED)
    int participantCount
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingRecordAdminResponse from(MeetingRecord meetingRecord, Team team, Section section) {
        return MeetingRecordAdminResponse.builder()
            .id(meetingRecord.getId())
            .sectionId(section.getId())
            .sectionName(section.getName())
            .teamId(team.getId())
            .teamName(team.getName())
            .phase(meetingRecord.getPhase())
            .authorId(meetingRecord.getAuthorId())
            .meetingAt(meetingRecord.getMeetingAt().format(FORMATTER))
            .location(meetingRecord.getLocation())
            .content(meetingRecord.getContent())
            .participantCount(meetingRecord.getParticipantCount())
            .build();
    }
}
