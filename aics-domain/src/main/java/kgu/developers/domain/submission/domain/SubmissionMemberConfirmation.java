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
    private int version;
    private boolean confirmedFinalReport;
    private boolean confirmedArtifacts;
    private String oneLineReview;
    private LocalDateTime confirmedAt;

    // 재제출로 새 버전이 올라오면, 예전 버전에 대한 확인은 지금 버전엔 유효하지 않다.
    public boolean confirmsVersion(int version) {
        return this.version == version;
    }

    public boolean isFullyConfirmed() {
        return this.confirmedFinalReport && this.confirmedArtifacts;
    }
}
