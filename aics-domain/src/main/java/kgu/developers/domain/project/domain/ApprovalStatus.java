package kgu.developers.domain.project.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalStatus {
    DRAFT("초안"),
    PENDING("미결정"),
    REVISION_REQUESTED("수정 요청"),
    APPROVED("승인"),
    REJECTED("거절");

    private final String description;
}
