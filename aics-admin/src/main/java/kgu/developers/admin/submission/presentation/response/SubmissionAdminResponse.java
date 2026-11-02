package kgu.developers.admin.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.team.domain.Team;

@Builder(toBuilder = true)
public record SubmissionAdminResponse(

        @Schema(description = "일반 제출(Submission) 식별자. 버전·아티팩트 조회에 사용하며 projectId·midReportId와 구분한다", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "팀 식별자", example = "10", requiredMode = REQUIRED)
        Long teamId,

        @Schema(description = "팀명", example = "A팀", requiredMode = REQUIRED)
        String teamName,

        @Schema(description = "프로젝트 주제(제안서 마일스톤이 아니거나 프로젝트가 없으면 null)", example = "AI 기반 팀 프로젝트 운영 플랫폼")
        String projectTitle,

        @Schema(description = "제안서(Project) 식별자. 제안서 마일스톤에서 문서가 존재할 때만 값이 있다", example = "10")
        Long projectId,

        @Schema(description = "제안서 제출 시각. 제안서가 미제출·수정요청 상태이거나 제안서 마일스톤이 아니면 null")
        LocalDateTime proposalCompletedAt,

        @Schema(description = "해당 팀과 마일스톤에 연결된 회의록 수", example = "2", requiredMode = REQUIRED)
        long meetingRecordCount,

        @Schema(description = "해당 팀이 작성한 전체 회의록 수(마일스톤 무관)", example = "5", requiredMode = REQUIRED)
        long totalMeetingRecordCount,

        @Schema(description = "마일스톤 식별자", example = "3", requiredMode = REQUIRED)
        Long milestoneId,

        @Schema(description = "제안서·중간보고서는 실제 문서 상태, 그 외에는 일반 제출 상태. 일반 제출 이력의 상태는 submissionStatus로 별도 제공한다", example = "SUBMITTED", requiredMode = REQUIRED)
        SubmissionStatus status,

        @Schema(description = "일반 제출(Submission) 상태. 문서 제출 여부를 나타내는 status와 별개이며, 일반 제출·재개방·완료 이력을 유지한다", example = "NOT_SUBMITTED", requiredMode = REQUIRED)
        SubmissionStatus submissionStatus,

        @Schema(description = "제안서·일반 제출은 SubmissionVersion 최신 버전 번호(0은 일반 제출 이력 없음). 제안서의 제출 여부는 status·proposalCompletedAt으로 판단한다. 중간보고서는 문서 수정 버전이며 제출 횟수가 아니다", example = "2", requiredMode = REQUIRED)
        int currentVersion,

        @Schema(description = "지금 제출 가능한지. 제안서는 목표·모든 섹션 작성 완료 및 미제출 여부로 판단하며 일정 제한을 적용하지 않는다. 그 외에는 마감/수정기간/조기오픈 계산 결과", example = "true", requiredMode = REQUIRED)
        boolean canSubmitNow,

        @Schema(description = "수정요청 대기 여부. 제안서·중간보고서는 문서 상태 기준이며, 중간보고서가 없거나 그 외 마일스톤이면 일반 제출 상태 기준", example = "false", requiredMode = REQUIRED)
        boolean hasPendingReview,

        @Schema(description = "해당 문서에 교수 계정이 남긴 피드백 메시지 수(완료 처리 알림 포함). 제안서·중간보고서 외에는 0", example = "2", requiredMode = REQUIRED)
        long feedbackCount,

        @Schema(description = "제출·재제출 또는 학생 답글이 마지막 교수 피드백보다 최신이어서 교수 피드백을 기다리는지. 제안서·중간보고서 외에는 false", example = "true", requiredMode = REQUIRED)
        boolean feedbackRequested,

        @Schema(description = "발표 순서(발표 마일스톤 전용, 없으면 null)", example = "3")
        Integer presentationOrder,

        @Schema(description = "일반 제출 단계의 COMPLETED 처리 시각(미완료면 null). 문서 제출 시각이 아니며 제안서 제출 시각은 proposalCompletedAt이다")
        LocalDateTime completedAt,

        @Schema(description = "일반 제출 단계를 COMPLETED 처리한 팀장 학번(미완료면 null)")
        String completedBy,

        @Schema(description = "중간보고서 식별자 (MID_REPORT 마일스톤 전용, 없으면 null)", example = "1")
        Long midReportId,

        @Schema(description = "팀장 이름(팀장이 없으면 null)", example = "홍길동")
        String leaderName
) {

    public static SubmissionAdminResponse of(Submission submission, Team team, boolean canSubmitNow, boolean hasPendingReview) {
        return of(submission, team, canSubmitNow, hasPendingReview, null, 0L, 0L);
    }

    public static SubmissionAdminResponse of(
            Submission submission,
            Team team,
            boolean canSubmitNow,
            boolean hasPendingReview,
            String projectTitle
    ) {
        return of(submission, team, canSubmitNow, hasPendingReview, projectTitle, 0L, 0L);
    }

    public static SubmissionAdminResponse of(
            Submission submission,
            Team team,
            boolean canSubmitNow,
            boolean hasPendingReview,
            String projectTitle,
            long meetingRecordCount
    ) {
        return of(submission, team, canSubmitNow, hasPendingReview, projectTitle, meetingRecordCount, 0L);
    }

    public static SubmissionAdminResponse of(
            Submission submission,
            Team team,
            boolean canSubmitNow,
            boolean hasPendingReview,
            String projectTitle,
            long meetingRecordCount,
            long totalMeetingRecordCount
    ) {
        return SubmissionAdminResponse.builder()
                .id(submission.getId())
                .teamId(team.getId())
                .teamName(team.getName())
                .projectTitle(projectTitle)
                .meetingRecordCount(meetingRecordCount)
                .totalMeetingRecordCount(totalMeetingRecordCount)
                .milestoneId(submission.getMilestoneId())
                .status(submission.getStatus())
                .submissionStatus(submission.getStatus())
                .currentVersion(submission.getCurrentVersion())
                .canSubmitNow(canSubmitNow)
                .hasPendingReview(hasPendingReview)
                .presentationOrder(submission.getPresentationOrder())
                .completedAt(submission.getCompletedAt())
                .completedBy(submission.getCompletedBy())
                .build();
    }

    public static SubmissionAdminResponse of(
            Submission submission,
            Team team,
            boolean canSubmitNow,
            boolean hasPendingReview,
            String projectTitle,
            long meetingRecordCount,
            Long midReportId,
            SubmissionStatus status,
            int currentVersion
    ) {
        return of(submission, team, canSubmitNow, hasPendingReview, projectTitle, meetingRecordCount, 0L, midReportId, status, currentVersion);
    }

    public static SubmissionAdminResponse of(
            Submission submission,
            Team team,
            boolean canSubmitNow,
            boolean hasPendingReview,
            String projectTitle,
            long meetingRecordCount,
            long totalMeetingRecordCount,
            Long midReportId,
            SubmissionStatus status,
            int currentVersion
    ) {
        return SubmissionAdminResponse.builder()
                .id(submission.getId())
                .teamId(team.getId())
                .teamName(team.getName())
                .projectTitle(projectTitle)
                .meetingRecordCount(meetingRecordCount)
                .totalMeetingRecordCount(totalMeetingRecordCount)
                .milestoneId(submission.getMilestoneId())
                .status(status != null ? status : submission.getStatus())
                .submissionStatus(submission.getStatus())
                .currentVersion(currentVersion)
                .canSubmitNow(canSubmitNow)
                .hasPendingReview(hasPendingReview)
                .presentationOrder(submission.getPresentationOrder())
                .completedAt(submission.getCompletedAt())
                .completedBy(submission.getCompletedBy())
                .midReportId(midReportId)
                .build();
    }
}
