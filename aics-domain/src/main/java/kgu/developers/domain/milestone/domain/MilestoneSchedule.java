package kgu.developers.domain.milestone.domain;

import java.time.LocalDateTime;

public record MilestoneSchedule(
        LocalDateTime opensAt,
        LocalDateTime dueAt,
        LocalDateTime lateSubmissionUntil,
        LocalDateTime revisionUntil,
        LocalDateTime evaluationOpensAt,
        LocalDateTime evaluationClosesAt
) {
    public MilestoneSchedule {
        if (dueAt == null) {
            throw new IllegalArgumentException("마감 시각은 필수입니다.");
        }

        if (opensAt != null && !opensAt.isBefore(dueAt)) {
            throw new IllegalArgumentException("작성 시작 시각은 마감 시각보다 빨라야 합니다.");
        }
        if (lateSubmissionUntil != null && lateSubmissionUntil.isBefore(dueAt)) {
            throw new IllegalArgumentException("지각 제출 종료 시각은 마감 시각보다 빠를 수 없습니다.");
        }
        if (revisionUntil != null && revisionUntil.isBefore(dueAt)) {
            throw new IllegalArgumentException("수정 종료 시각은 마감 시각보다 빠를 수 없습니다.");
        }
        if (lateSubmissionUntil != null && revisionUntil != null
                && revisionUntil.isBefore(lateSubmissionUntil)) {
            throw new IllegalArgumentException("수정 종료 시각은 지각 제출 종료 시각보다 빠를 수 없습니다.");
        }

        boolean hasEvaluationStart = evaluationOpensAt != null;
        boolean hasEvaluationEnd = evaluationClosesAt != null;
        if (hasEvaluationStart != hasEvaluationEnd) {
            throw new IllegalArgumentException("평가 시작 시각과 종료 시각은 함께 설정해야 합니다.");
        }
        if (hasEvaluationStart && evaluationOpensAt.isBefore(dueAt)) {
            throw new IllegalArgumentException("평가 시작 시각은 마감 시각보다 빠를 수 없습니다.");
        }
        LocalDateTime submissionOrRevisionUntil = revisionUntil != null
                ? revisionUntil
                : lateSubmissionUntil;
        if (hasEvaluationStart && submissionOrRevisionUntil != null
                && evaluationOpensAt.isBefore(submissionOrRevisionUntil)) {
            throw new IllegalArgumentException("평가 시작 시각은 제출·수정 종료 시각보다 빠를 수 없습니다.");
        }
        if (hasEvaluationStart && !evaluationOpensAt.isBefore(evaluationClosesAt)) {
            throw new IllegalArgumentException("평가 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }

    public MilestoneSchedule withEvaluationWindow(
            LocalDateTime evaluationOpensAt,
            LocalDateTime evaluationClosesAt
    ) {
        return new MilestoneSchedule(
                opensAt,
                dueAt,
                lateSubmissionUntil,
                revisionUntil,
                evaluationOpensAt,
                evaluationClosesAt
        );
    }
}
