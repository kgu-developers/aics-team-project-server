package kgu.developers.api.meetingrecord.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import lombok.Builder;

@Builder
public record MeetingRecordCreateRequest(

    // TODO: 인증 인프라(aics-auth) 구현 후에는 SecurityContext의 인증된 사용자로 대체한다.
    //  현재는 인증 principal이 없어 요청 값으로 받는다.
    @Schema(description = "작성자 학번", example = "202412345", requiredMode = REQUIRED)
    @NotBlank
    String authorId,

    @Schema(description = "회의 일시", example = "2026-08-03T14:00:00", requiredMode = REQUIRED)
    @NotNull
    LocalDateTime meetingAt,

    @Schema(description = "장소/진행방식", example = "온라인(Zoom)")
    String location,

    @Schema(description = "회의 단계(PROPOSAL:제안, MID_CHECK:중간, FINAL:최종)", example = "MID_CHECK", requiredMode = REQUIRED)
    @NotNull
    MeetingPhase phase,

    @Schema(description = "회의 내용", example = "이번 주 진행 상황 공유 및 다음 마일스톤 논의", requiredMode = REQUIRED)
    @NotBlank
    String content,

    @Schema(description = "참석자 학번 목록", example = "[\"202412345\", \"202412346\"]")
    List<String> participantIds
) {
}
