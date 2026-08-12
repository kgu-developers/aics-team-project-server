package kgu.developers.domain.evaluation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeerEvaluationQuestionType {
    SCALE("점수형"),
    TEXT("서술형");

    private final String description;
}
