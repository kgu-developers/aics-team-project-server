package kgu.developers.domain.editlock.domain;

/**
 * 폴리모픽 참조 대상(AuditLog·TeamMessage.related_type과 같은 패턴, FK 없음).
 * PRD상 후보는 PROJECT(B2). PRESENTATION_CONTENT(B3)는 KD3-214에서 그 도메인 자체가
 * 폐기되며 함께 제거됨. 다른 공동편집 폼이 추가되면 값만 늘리면 된다.
 */
public enum EditLockTargetType {
    PROJECT
}
