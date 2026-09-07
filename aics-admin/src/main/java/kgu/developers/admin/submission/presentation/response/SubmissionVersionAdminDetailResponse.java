package kgu.developers.admin.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.user.domain.User;

@Builder
public record SubmissionVersionAdminDetailResponse(

        @Schema(description = "버전 번호", example = "2", requiredMode = REQUIRED)
        int version,

        @Schema(description = "변경 요약", example = "로그인 버그 수정")
        String description,

        @Schema(description = "이번 버전에서 뭘 바꿨는지", example = "회원가입 화면 유효성 검사 로직 추가")
        String changeNote,

        @Schema(description = "제출한 사람", requiredMode = REQUIRED)
        SubmissionSubmitterAdminResponse submittedBy,

        @Schema(description = "제출 일시", requiredMode = REQUIRED)
        LocalDateTime submittedAt,

        @Schema(description = "마감 이후 지각 제출 여부", example = "false", requiredMode = REQUIRED)
        boolean late,

        @Schema(description = "이 버전에 포함된 아티팩트 목록", requiredMode = REQUIRED)
        List<SubmissionArtifactAdminResponse> artifacts
) {

    public static SubmissionVersionAdminDetailResponse of(
            SubmissionVersion version, User submitter, List<SubmissionArtifactAdminResponse> artifacts) {
        return SubmissionVersionAdminDetailResponse.builder()
                .version(version.getVersion())
                .description(version.getDescription())
                .changeNote(version.getChangeNote())
                .submittedBy(SubmissionSubmitterAdminResponse.of(version.getSubmittedBy(), submitter))
                .submittedAt(version.getSubmittedAt())
                .late(version.isLate())
                .artifacts(artifacts)
                .build();
    }
}
