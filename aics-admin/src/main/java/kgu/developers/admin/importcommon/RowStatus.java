package kgu.developers.admin.importcommon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RowStatus {
    VALID("이미 가입된 학생, 수강 등록 예정"),
    NEW_USER("미가입 학생, 계정 생성 후 수강 등록 예정"),
    UPDATE("이미 편성됨, 팀장·역할만 변경 예정"),
    DUPLICATE("이미 등록됨 (반영 시 건너뜀)"),
    INVALID("오류 (반영 불가)");

    private final String description;
}
