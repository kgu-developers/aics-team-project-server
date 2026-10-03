package kgu.developers.domain.meetingrecord.domain;

// 회의록 본문 없이 팀별 집계만 필요한 화면(분반 산출물 현황)을 위한 값.
// editCount는 낙관적 락 version 합이라 "수정 횟수"의 근사값이다.
public record MeetingRecordStats(long recordCount, long editCount) {
    public static final MeetingRecordStats NONE = new MeetingRecordStats(0L, 0L);
}
