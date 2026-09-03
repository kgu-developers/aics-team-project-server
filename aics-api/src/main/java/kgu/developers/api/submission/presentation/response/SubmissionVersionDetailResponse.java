package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.format.DateTimeFormatter;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionVersion;

@Builder
public record SubmissionVersionDetailResponse(

        @Schema(description = "버전 번호", example = "2", requiredMode = REQUIRED)
        int version,

        @Schema(description = "변경 요약", example = "로그인 버그 수정")
        String description,

        @Schema(description = "이번 버전에서 뭘 바꿨는지", example = "회원가입 화면 유효성 검사 로직 추가")
        String changeNote,

        @Schema(description = "제출한 학번", example = "202412345", requiredMode = REQUIRED)
        String submittedBy,

        @Schema(description = "제출 일시", example = "2026-09-02 14:00", requiredMode = REQUIRED)
        String submittedAt,

        @Schema(description = "마감 이후 지각 제출 여부", example = "false", requiredMode = REQUIRED)
        boolean late,

        @Schema(description = "이 버전에 포함된 아티팩트 목록", requiredMode = REQUIRED)
        List<SubmissionArtifactResponse> artifacts
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static SubmissionVersionDetailResponse of(SubmissionVersion version, List<SubmissionArtifactResponse> artifacts) {
        return SubmissionVersionDetailResponse.builder()
                .version(version.getVersion())
                .description(version.getDescription())
                .changeNote(version.getChangeNote())
                .submittedBy(version.getSubmittedBy())
                .submittedAt(version.getSubmittedAt().format(FORMATTER))
                .late(version.isLate())
                .artifacts(artifacts)
                .build();
    }
}
