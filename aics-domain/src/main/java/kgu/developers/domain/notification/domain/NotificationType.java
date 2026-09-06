package kgu.developers.domain.notification.domain;

/**
 * 이후 B1(리뷰 알림)·F(미제출 알림) 등이 값을 추가해나갈 것을 전제로 한 확장용 enum.
 * 지금은 D(공지사항 브로드캐스트)에 필요한 값만 둔다.
 */
public enum NotificationType {
    SECTION_ANNOUNCEMENT,
    PRE_SURVEY_PREFERRED_PEER_REQUESTED,  // 사전조사에서 나를 조원으로 지목함
    PRE_SURVEY_PREFERRED_PEER_CANCELLED,  // 나를 지목했던 학생이 지목을 취소하거나 다른 학생으로 바꿈
    PRE_SURVEY_PREFERRED_PEER_DECIDED     // 내가 지목한 학생이 수락하거나 거절함
}
