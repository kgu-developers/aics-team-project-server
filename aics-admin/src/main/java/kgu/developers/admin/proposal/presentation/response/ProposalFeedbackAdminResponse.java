package kgu.developers.admin.proposal.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import lombok.Builder;

@Builder
public record ProposalFeedbackAdminResponse(
    @Schema(description = "메시지 식별자", example = "100")
    Long messageId,

    @Schema(description = "팀 식별자", example = "10")
    Long teamId,

    @Schema(description = "제안서(프로젝트) 식별자", example = "1")
    Long projectId,

    @Schema(description = "작성자(교수) 학번", example = "202699999")
    String senderId,

    @Schema(description = "작성자(교수) 이름", example = "김교수")
    String senderName,

    @Schema(description = "피드백 내용", example = "데이터 구성의 수집 방법을 구체적으로 적어 주세요.")
    String message,

    @Schema(description = "작성 일시", example = "2026-09-13 14:00")
    String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ProposalFeedbackAdminResponse of(TeamMessage teamMessage, Long teamId, Long projectId, String senderName) {
        return ProposalFeedbackAdminResponse.builder()
            .messageId(teamMessage.getId())
            .teamId(teamId)
            .projectId(projectId)
            .senderId(teamMessage.getSenderId())
            .senderName(senderName)
            .message(teamMessage.getMessage())
            .createdAt(teamMessage.getCreatedAt() != null ? teamMessage.getCreatedAt().format(FORMATTER) : null)
            .build();
    }
}
