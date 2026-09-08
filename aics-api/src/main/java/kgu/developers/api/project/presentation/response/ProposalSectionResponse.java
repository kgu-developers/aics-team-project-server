package kgu.developers.api.project.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionType;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

public record ProposalSectionResponse(
    @Schema(description = "섹션", requiredMode = REQUIRED)
    ProposalSectionType section,

    @Schema(description = "담당 팀원 학번 (미지정이면 null)")
    String assigneeUserId,

    @Schema(description = "담당 팀원 이름 (미지정이면 null)")
    String assigneeName,

    @Schema(description = "작성 완료 여부", requiredMode = REQUIRED)
    boolean completed,

    @Schema(description = "작성 완료 시각 (미완료면 null)")
    LocalDateTime completedAt
) {
    /**
     * 아직 아무도 담당·완료 처리를 하지 않은 섹션은 행이 없다. 고정 구성이라 응답에서는 빈 상태로 채워 보낸다.
     */
    public static ProposalSectionResponse empty(ProposalSectionType section) {
        return new ProposalSectionResponse(section, null, null, false, null);
    }

    public static ProposalSectionResponse of(ProposalSection section, String assigneeName) {
        return new ProposalSectionResponse(
            section.getType(),
            section.getAssigneeUserId(),
            assigneeName,
            section.isCompleted(),
            section.getCompletedAt()
        );
    }
}
