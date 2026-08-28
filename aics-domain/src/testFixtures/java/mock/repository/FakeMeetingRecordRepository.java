package mock.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    public Page<MeetingRecord> findAllByTeamIdIn(List<Long> teamIds, Pageable pageable) {
        List<MeetingRecord> records = store.values().stream()
            .filter(meetingRecord -> teamIds.contains(meetingRecord.getTeamId()))
            .sorted(Comparator.comparing(MeetingRecord::getMeetingAt).reversed()
                .thenComparing(MeetingRecord::getId, Comparator.reverseOrder()))
            .toList();
        int start = Math.min((int) pageable.getOffset(), records.size());
        int end = Math.min(start + pageable.getPageSize(), records.size());
        return new PageImpl<>(records.subList(start, end), pageable, records.size());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
