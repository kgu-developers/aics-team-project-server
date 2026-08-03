package kgu.developers.domain.meetingrecord.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingRecord {

    private Long id;
    private Long teamId;
    private MeetingPhase phase;
    private String authorId;
    private LocalDateTime meetingAt;
    private String location;
    private String content;
    private List<MeetingParticipant> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 개인별 작업량을 감시/수치화하지 않는다는 설계 원칙에 따라, 참석자는 "누가 참석했는지"만 기록한다.
    public static MeetingRecord create(
        Long teamId,
        MeetingPhase phase,
        String authorId,
        LocalDateTime meetingAt,
        String location,
        String content,
        List<String> participantUserIds
    ) {
        return MeetingRecord.builder()
            .teamId(teamId)
            .phase(phase)
            .authorId(authorId)
            .meetingAt(meetingAt)
            .location(location)
            .content(content)
            .participants(toParticipants(null, participantUserIds))
            .build();
    }

    public static List<MeetingParticipant> toParticipants(Long meetingRecordId, List<String> userIds) {
        if (userIds == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(userIds)).stream()
            .map(userId -> MeetingParticipant.create(meetingRecordId, userId))
            .toList();
    }

    public void updateMeetingAt(LocalDateTime meetingAt) {
        this.meetingAt = meetingAt;
    }

    public void updateLocation(String location) {
        this.location = location;
    }

    public void updatePhase(MeetingPhase phase) {
        this.phase = phase;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateParticipants(List<String> participantUserIds) {
        this.participants = toParticipants(this.id, participantUserIds);
    }

    public int getParticipantCount() {
        return participants == null ? 0 : participants.size();
    }
}
