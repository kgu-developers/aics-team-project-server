package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.domain.submission.domain.Submission;

@Builder
public record TeamPresentationResponse(

        @Schema(description = "팀 식별자", example = "10", requiredMode = REQUIRED)
        Long teamId,

        @Schema(description = "팀명", example = "1조")
        String teamName,

        @Schema(description = "제출 식별자", example = "1", requiredMode = REQUIRED)
        Long submissionId,

        @Schema(description = "발표 순서(미지정이면 null)", example = "1")
        Integer presentationOrder,

        @Schema(description = "프로젝트 제안서 정보(미작성이면 null)")
        ProjectResponse project,

        @Schema(description = "제출된 최신 산출물 목록(PDF 다운로드 임시 URL, 시연영상 링크 등)", requiredMode = REQUIRED)
        List<SubmissionArtifactResponse> artifacts
) {

    public static TeamPresentationResponse of(
            Submission submission,
            String teamName,
            ProjectResponse project,
            List<SubmissionArtifactResponse> artifacts
    ) {
        return TeamPresentationResponse.builder()
                .teamId(submission.getTeamId())
                .teamName(teamName)
                .submissionId(submission.getId())
                .presentationOrder(submission.getPresentationOrder())
                .project(project)
                .artifacts(artifacts == null ? List.of() : artifacts)
                .build();
    }
}
