package kgu.developers.domain.teammessage.domain;

/**
 * team_message.related_type — 메시지가 어떤 맥락에서 발생했는지를 나타내는 폴리모픽 태그.
 * related_id는 이 값에 따라 참조 대상 테이블이 달라지며, FK가 없는 순수 id 컬럼이다.
 */
public enum TeamMessageRelatedType {
    PROPOSAL,
    MEETING,
    MID_REPORT,
    FINAL_SUBMISSION,
    REVIEW,
    QUESTION,
    GENERAL
}
