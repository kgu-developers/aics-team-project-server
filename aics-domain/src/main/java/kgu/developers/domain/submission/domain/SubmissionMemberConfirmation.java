package kgu.developers.domain.submission.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionMemberConfirmation {
    private Long id;
    private Long submissionId;
    private String userId;
    private boolean confirmedFinalReport;
    private boolean confirmedArtifacts;
    private String oneLineReview;
    private LocalDateTime confirmedAt;

    public static SubmissionMemberConfirmation create(
            Long submissionId,
            String userId,
            boolean confirmedFinalReport,
            boolean confirmedArtifacts,
            String oneLineReview
    ) {
        return SubmissionMemberConfirmation.builder()
                .submissionId(submissionId)
                .userId(userId)
                .confirmedFinalReport(confirmedFinalReport)
                .confirmedArtifacts(confirmedArtifacts)
                .oneLineReview(oneLineReview)
                .confirmedAt(LocalDateTime.now())
                .build();
    }
}
