package kgu.developers.domain.meetingrecord.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FakeMeetingRecordRepository implements MeetingRecordRepository {

    private final Map<Long, MeetingRecord> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public MeetingRecord save(MeetingRecord meetingRecord) {
        Long id = meetingRecord.getId() != null ? meetingRecord.getId() : sequence.incrementAndGet();

        List<MeetingParticipant> participants = meetingRecord.getParticipants() == null
            ? new ArrayList<>()
            : meetingRecord.getParticipants().stream()
                .map(participant -> MeetingParticipant.builder()
                    .id(participant.getId())
                    .meetingRecordId(id)
                    .userId(participant.getUserId())
                    .build())
                .toList();

        LocalDateTime createdAt = meetingRecord.getCreatedAt() != null ? meetingRecord.getCreatedAt() : LocalDateTime.now();

        MeetingRecord saved = MeetingRecord.builder()
            .id(id)
            .teamId(meetingRecord.getTeamId())
            .phase(meetingRecord.getPhase())
            .authorId(meetingRecord.getAuthorId())
            .meetingAt(meetingRecord.getMeetingAt())
            .location(meetingRecord.getLocation())
            .content(meetingRecord.getContent())
            .participants(participants)
            .createdAt(createdAt)
            .updatedAt(LocalDateTime.now())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<MeetingRecord> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<MeetingRecord> findAllByTeamId(Long teamId, MeetingPhase phase) {
        return store.values().stream()
            .filter(meetingRecord -> meetingRecord.getTeamId().equals(teamId))
            .filter(meetingRecord -> phase == null || meetingRecord.getPhase() == phase)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
