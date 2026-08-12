package kgu.developers.domain.enrollment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusType {
    ACTIVE("활성"),
    WITHDRAWN("탈퇴");


    private final String description;
}
