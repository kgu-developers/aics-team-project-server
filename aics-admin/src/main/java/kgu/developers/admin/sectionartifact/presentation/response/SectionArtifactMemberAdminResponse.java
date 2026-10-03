package kgu.developers.admin.sectionartifact.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.application.query.SectionArtifactMember;

@Builder
public record SectionArtifactMemberAdminResponse(
        @Schema(description = "학번", example = "20261234", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "이름(탈퇴한 팀원은 대체 문구)", example = "김철수", requiredMode = REQUIRED)
        String name
) {

    public static SectionArtifactMemberAdminResponse from(SectionArtifactMember member) {
        return SectionArtifactMemberAdminResponse.builder()
                .studentNumber(member.studentNumber())
                .name(member.displayName())
                .build();
    }
}
