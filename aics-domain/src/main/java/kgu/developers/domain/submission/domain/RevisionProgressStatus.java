package kgu.developers.domain.submission.domain;

// 리뷰(B1)에서 REVISION_REQUESTED가 걸린 뒤 재제출까지의 진행 상태.
// Phase 1(제출/조회)에서는 필드만 갖고 다니고 실제로 전이시키지 않는다 — 리뷰 연동은 이후 단계에서.
public enum RevisionProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
