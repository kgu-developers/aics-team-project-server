package kgu.developers.domain.evaluation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationTeammateAnswer {
    private Long id;
    private Long submissionId;
    private String targetUserId;
    private int contributionPercent;
    private String contributionDetail;
    private String teammateAssessment;

    public static PeerEvaluationTeammateAnswer create(
        Long submissionId,
        String targetUserId,
        int contributionPercent,
        String contributionDetail,
        String teammateAssessment
    ) {
        if (contributionPercent < 0 || contributionPercent > 100) {
            throw new IllegalArgumentException("팀원 기여도는 0 이상 100 이하이어야 합니다.");
        }
        return PeerEvaluationTeammateAnswer.builder()
            .submissionId(submissionId)
            .targetUserId(targetUserId)
            .contributionPercent(contributionPercent)
            .contributionDetail(normalize(contributionDetail))
            .teammateAssessment(normalize(teammateAssessment))
            .build();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() > 2000) {
            throw new IllegalArgumentException("팀원 평가 답변은 2000자를 넘을 수 없습니다.");
        }
        return normalized;
    }
}
