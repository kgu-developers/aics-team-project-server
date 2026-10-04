package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    public Page<MeetingAction> findAllByTeamIdIn(
        List<Long> teamIds,
        Long meetingRecordId,
        MeetingActionStatus status,
        Pageable pageable
    ) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<MeetingAction> filtered = store.values().stream()
            .filter(action -> teamIds.contains(teamIdOf(action.getMeetingRecordId())))
            .filter(action -> meetingRecordId == null || meetingRecordId.equals(action.getMeetingRecordId()))
            .filter(action -> status == null || action.getStatus() == status)
            .sorted(comparator(pageable))
            .toList();

        int from = (int) Math.min(pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(from, to), pageable, filtered.size());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    // 실제 리포지토리는 Pageable의 정렬을 그대로 쓰므로, Fake도 정렬을 무시하지 않고 따라간다.
    private Comparator<MeetingAction> comparator(Pageable pageable) {
        Comparator<MeetingAction> comparator = null;
        for (Sort.Order order : pageable.getSort()) {
            Comparator<MeetingAction> current = switch (order.getProperty()) {
                case "createdAt" -> Comparator.comparing(
                    MeetingAction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
                case "dueAt" -> Comparator.comparing(
                    MeetingAction::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()));
                default -> Comparator.comparing(MeetingAction::getId);
            };
            if (order.isDescending()) {
                current = current.reversed();
            }
            comparator = comparator == null ? current : comparator.thenComparing(current);
        }
        return comparator == null ? Comparator.comparing(MeetingAction::getId) : comparator;
    }

    private Long teamIdOf(Long meetingRecordId) {
        return meetingRecordRepository.findById(meetingRecordId)
            .map(record -> record.getTeamId())
            .orElse(null);
    }
}
