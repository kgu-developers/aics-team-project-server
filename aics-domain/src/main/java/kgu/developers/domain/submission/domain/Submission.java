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
public class Submission {
    private Long id;
    private Long teamId;
    private Long milestoneId;
    private SubmissionStatus status;
    private int currentVersion;
    private LocalDateTime revisionDueAt;
    private RevisionProgressStatus revisionProgress;
    private LocalDateTime reopenedAt;
    private String reopenedBy;
    private Integer presentationOrder;
    private LocalDateTime completedAt;
    private String completedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // 마일스톤 publish 시점에 만드는 게 아니라, 팀이 그 마일스톤을 처음 조회하는 시점에
    // lazy get-or-create로 만든다(SubmissionQueryService 참고) — 항상 not_submitted로 시작.
    public static Submission create(Long teamId, Long milestoneId) {
        return Submission.builder()
                .teamId(teamId)
                .milestoneId(milestoneId)
                .status(SubmissionStatus.NOT_SUBMITTED)
                .currentVersion(0)
                .build();
    }

    public void recordNewVersion(int newVersion) {
        this.currentVersion = newVersion;
        this.status = SubmissionStatus.SUBMITTED;
    }

    // 교수가 완료된 단계를 다시 열어서, 그 시각까지는 팀이 다시 제출할 수 있게 한다.
    // "재오픈이 정확히 뭘 되돌리는가"는 PRD 미결정사항(#3)이라, 우선 이 팀·이 마일스톤에
    // 한해 revisionDueAt까지 재제출을 허용하는 걸로 최소 구현해뒀다 — 팀 결정 나오면 갱신 필요.
    public void reopen(String reopenedBy, LocalDateTime revisionDueAt) {
        this.reopenedAt = LocalDateTime.now();
        this.reopenedBy = reopenedBy;
        this.revisionDueAt = revisionDueAt;
        this.status = SubmissionStatus.REVISION_REQUESTED;
    }

    public void assignPresentationOrder(Integer order) {
        this.presentationOrder = order;
    }

    public boolean belongsToTeam(Long teamId) {
        return this.teamId.equals(teamId);
    }

    public boolean isSubmitted() {
        return this.status != SubmissionStatus.NOT_SUBMITTED;
    }

    public boolean isCompleted() {
        return this.status == SubmissionStatus.COMPLETED;
    }

    public void complete(String completedBy) {
        this.status = SubmissionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedBy = completedBy;
    }
}
