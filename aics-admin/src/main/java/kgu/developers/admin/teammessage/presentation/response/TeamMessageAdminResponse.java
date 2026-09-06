package kgu.developers.admin.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.Builder;

@Builder
public record TeamMessageAdminResponse(

    @Schema(description = "메시지 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "분반 식별자", example = "10", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "분반명", example = "1151", requiredMode = REQUIRED)
    String sectionName,

    @Schema(description = "팀 식별자", example = "20", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "A팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "발신자 학번", example = "202412345", requiredMode = REQUIRED)
    String senderId,

    @Schema(description = "메시지 본문", example = "화면설계서 확인 부탁드립니다.", requiredMode = REQUIRED)
    String message,

    @Schema(description = "메시지 관련 유형", example = "GENERAL", requiredMode = REQUIRED)
    TeamMessageRelatedType relatedType,

    @Schema(description = "관련 리소스 식별자", example = "1", requiredMode = NOT_REQUIRED)
    Long relatedId,

    @Schema(description = "중요 표시 여부", example = "false", requiredMode = REQUIRED)
    boolean important,

    @Schema(description = "요청한 관리자의 읽음 여부", example = "false", requiredMode = REQUIRED)
    boolean read,

    @Schema(description = "생성일시", example = "2026-08-25 19:30", requiredMode = REQUIRED)
    String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static TeamMessageAdminResponse from(
        TeamMessage teamMessage,
        Team team,
        Section section,
        boolean read
    ) {
        return TeamMessageAdminResponse.builder()
            .id(teamMessage.getId())
            .sectionId(section.getId())
            .sectionName(section.getName())
            .teamId(team.getId())
            .teamName(team.getName())
            .senderId(teamMessage.getSenderId())
            .message(teamMessage.getMessage())
            .relatedType(teamMessage.getRelatedType())
            .relatedId(teamMessage.getRelatedId())
            .important(teamMessage.isImportant())
            .read(read)
            .createdAt(teamMessage.getCreatedAt() != null ? teamMessage.getCreatedAt().format(FORMATTER) : null)
            .build();
    }
}
