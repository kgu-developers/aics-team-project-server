package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Team;
import lombok.Builder;

@Builder
public record MeetingRecordAdminDetailResponse(

    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "분반 식별자", example = "10", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "요일·시간/과목번호 표시 문자열", example = "월123/1154", requiredMode = REQUIRED)
    String sectionName,

    @Schema(description = "팀 식별자", example = "20", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "A팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "회의록 제목", example = "3주차 정기 회의", requiredMode = REQUIRED)
    String title,

    @Schema(description = "회의 단계(PROPOSAL:제안, MID_CHECK:중간, FINAL:최종)", example = "MID_CHECK", requiredMode = REQUIRED)
    MeetingPhase phase,

    @Schema(description = "작성자 학번", example = "202412345", requiredMode = REQUIRED)
    String authorId,

    @Schema(description = "회의 일시", example = "2026-08-03 14:00", requiredMode = REQUIRED)
    String meetingAt,

    @Schema(description = "장소/진행방식", example = "온라인(Zoom)")
    String location,

    @Schema(description = "회의 내용", example = "이번 주 진행 상황 공유 및 다음 마일스톤 논의", requiredMode = REQUIRED)
    String content,

    @Schema(description = "참석자 학번 목록", requiredMode = REQUIRED)
    List<String> participantIds,

    @Schema(description = "생성일", example = "2026-08-01 10:00", requiredMode = REQUIRED)
    String createdAt,

    @Schema(description = "수정일", example = "2026-08-02 09:30", requiredMode = REQUIRED)
    String updatedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingRecordAdminDetailResponse from(
        MeetingRecord meetingRecord,
        Team team,
        Section section
    ) {
        return MeetingRecordAdminDetailResponse.builder()
            .id(meetingRecord.getId())
            .sectionId(section.getId())
            .sectionName(section.getName())
            .teamId(team.getId())
            .teamName(team.getName())
            .title(meetingRecord.getTitle())
            .phase(meetingRecord.getPhase())
            .authorId(meetingRecord.getAuthorId())
            .meetingAt(meetingRecord.getMeetingAt().format(FORMATTER))
            .location(meetingRecord.getLocation())
            .content(meetingRecord.getContent())
            .participantIds(meetingRecord.getParticipants().stream()
                .map(MeetingParticipant::getUserId)
                .toList())
            .createdAt(meetingRecord.getCreatedAt().format(FORMATTER))
            .updatedAt(meetingRecord.getUpdatedAt().format(FORMATTER))
            .build();
    }
}
