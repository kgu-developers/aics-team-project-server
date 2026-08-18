package kgu.developers.domain.feedback.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewResultStatus {
    APPROVED("승인"),
    FEEDBACK_PROVIDED("피드백 제공"),
    REVISION_REQUESTED("수정 요청");

    private final String description;
}
