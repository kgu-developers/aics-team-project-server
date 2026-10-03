package kgu.developers.domain.meetingrecord.domain;

import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordInvalidEditReasonException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingRecordEditLog {

    public static final int MIN_REASON_LENGTH = 30;
    public static final int MAX_REASON_LENGTH = 500;

    private Long id;
    private Long meetingRecordId;
    // 분반 산출물 집계가 팀 단위라 조인을 매번 걸지 않도록 팀 식별자를 함께 적재한다.
    private Long teamId;
    private String editorId;
    private String reason;
    private LocalDateTime createdAt;

    public static MeetingRecordEditLog create(Long meetingRecordId, Long teamId, String editorId, String reason) {
        return MeetingRecordEditLog.builder()
            .meetingRecordId(meetingRecordId)
            .teamId(teamId)
            .editorId(editorId)
            .reason(normalizeReason(reason))
            .build();
    }

    // 길이는 공백을 걷어낸 뒤 센다. 앞뒤 공백으로 글자 수를 채우거나, 반대로 공백 때문에
    // 상한을 넘겨 거절되는 일이 없도록 저장값 자체를 트림한 문자열로 맞춘다.
    private static String normalizeReason(String reason) {
        if (reason == null) {
            throw new MeetingRecordInvalidEditReasonException();
        }
        String normalized = reason.trim();
        if (normalized.length() < MIN_REASON_LENGTH || normalized.length() > MAX_REASON_LENGTH) {
            throw new MeetingRecordInvalidEditReasonException();
        }
        return normalized;
    }
}
