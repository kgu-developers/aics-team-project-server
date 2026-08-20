package kgu.developers.domain.meetingrecord.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingParticipant {

    private Long id;
    private Long meetingRecordId;
    private String userId;

    public static MeetingParticipant create(Long meetingRecordId, String userId) {
        return MeetingParticipant.builder()
            .meetingRecordId(meetingRecordId)
            .userId(userId)
            .build();
    }
}
