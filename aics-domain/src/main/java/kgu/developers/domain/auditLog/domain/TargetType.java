package kgu.developers.domain.auditLog.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetType {
    TEAM(1L, "팀"),
    USER(2L, "사용자"),
    MEETING_RECORD(3L, "회의록"),
    SUBMISSION(4L, "제출물"),
    MILESTONE(5L, "마일스톤");

    private final Long code;
    private final String description;

    public static TargetType fromCode(Long code) {
        if (code == null) {
            throw new IllegalArgumentException("TargetType의 코드는 NULL일 수 없습니다.");
        }
        for (TargetType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 TargetType 코드: " + code);
    }
}