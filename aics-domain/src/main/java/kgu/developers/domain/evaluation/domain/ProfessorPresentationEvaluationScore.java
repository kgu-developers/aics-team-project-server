package kgu.developers.domain.evaluation.domain;

public record ProfessorPresentationEvaluationScore(
        Long id, Long evaluationId, Long criterionId, int score) {
    public ProfessorPresentationEvaluationScore {
        if (evaluationId == null || evaluationId <= 0 || criterionId == null || criterionId <= 0
                || score < 0) {
            throw new IllegalArgumentException("교수자 발표 평가 점수 정보가 올바르지 않습니다.");
        }
    }

    public static ProfessorPresentationEvaluationScore create(Long evaluationId,
            Long criterionId, int score, int maxScore) {
        if (score > maxScore) {
            throw new IllegalArgumentException("점수는 항목 최대 점수를 초과할 수 없습니다.");
        }
        return new ProfessorPresentationEvaluationScore(null, evaluationId, criterionId, score);
    }
}
