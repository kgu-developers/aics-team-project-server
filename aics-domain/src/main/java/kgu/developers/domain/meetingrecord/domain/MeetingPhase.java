package kgu.developers.domain.meetingrecord.domain;

// 회의 단계: PROPOSAL(제안), MID_CHECK(중간), FINAL(최종) — milestone_id 강결합 대신 느슨한 phase 문자열 연결(PRD C시트 반영)
public enum MeetingPhase {
    PROPOSAL,
    MID_CHECK,
    FINAL
}
