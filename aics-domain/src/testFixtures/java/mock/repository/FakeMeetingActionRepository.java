package mock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;

public class FakeMeetingActionRepository implements MeetingActionRepository {

    private final Map<Long, MeetingAction> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);
    private final MeetingRecordRepository meetingRecordRepository;

    public FakeMeetingActionRepository(MeetingRecordRepository meetingRecordRepository) {
        this.meetingRecordRepository = meetingRecordRepository;
    }

    @Override
    public MeetingAction save(MeetingAction meetingAction) {
        Long id = meetingAction.getId() != null ? meetingAction.getId() : sequence.incrementAndGet();
        LocalDateTime createdAt = meetingAction.getCreatedAt() != null ? meetingAction.getCreatedAt() : LocalDateTime.now();

        MeetingAction saved = MeetingAction.builder()
            .id(id)
            .meetingRecordId(meetingAction.getMeetingRecordId())
            .assigneeId(meetingAction.getAssigneeId())
            .content(meetingAction.getContent())
            .status(meetingAction.getStatus())
            .dueAt(meetingAction.getDueAt())
            .createdAt(createdAt)
            .updatedAt(LocalDateTime.now())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<MeetingAction> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<MeetingAction> findAllByMeetingRecordId(Long meetingRecordId) {
        return store.values().stream()
            .filter(action -> action.getMeetingRecordId().equals(meetingRecordId))
            .toList();
    }

    @Override
    public List<MeetingAction> findAllByTeamId(Long teamId, MeetingActionStatus status) {
        return store.values().stream()
            .filter(action -> teamId.equals(teamIdOf(action.getMeetingRecordId())))
            .filter(action -> status == null || action.getStatus() == status)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    private Long teamIdOf(Long meetingRecordId) {
        return meetingRecordRepository.findById(meetingRecordId)
            .map(record -> record.getTeamId())
            .orElse(null);
    }
}
