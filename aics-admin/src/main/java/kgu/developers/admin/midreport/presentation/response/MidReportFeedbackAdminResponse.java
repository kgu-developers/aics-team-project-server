package kgu.developers.admin.midreport.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import lombok.Builder;

@Builder
public record MidReportFeedbackAdminResponse(
    @Schema(description = "메시지 식별자", example = "100")
    Long messageId,

    @Schema(description = "팀 식별자", example = "10")
    Long teamId,

    @Schema(description = "중간보고서 식별자", example = "1")
    Long midReportId,

    @Schema(description = "작성자(교수) 학번", example = "202699999")
    String senderId,

    @Schema(description = "작성자(교수) 이름", example = "김교수")
    String senderName,

    @Schema(description = "피드백 내용", example = "GUI 화면 흐름을 보완해 주세요.")
    String message,

    @Schema(description = "작성 일시", example = "2026-09-13 14:00")
    String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MidReportFeedbackAdminResponse of(TeamMessage teamMessage, Long teamId, Long midReportId, String senderName) {
        return MidReportFeedbackAdminResponse.builder()
            .messageId(teamMessage.getId())
            .teamId(teamId)
            .midReportId(midReportId)
            .senderId(teamMessage.getSenderId())
            .senderName(senderName)
            .message(teamMessage.getMessage())
            .createdAt(teamMessage.getCreatedAt() != null ? teamMessage.getCreatedAt().format(FORMATTER) : null)
            .build();
    }
}
