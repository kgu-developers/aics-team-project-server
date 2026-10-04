package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class FakeMeetingRecordEditLogRepository implements MeetingRecordEditLogRepository {

    private final Map<Long, MeetingRecordEditLog> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public MeetingRecordEditLog save(MeetingRecordEditLog meetingRecordEditLog) {
        Long id = meetingRecordEditLog.getId() != null
            ? meetingRecordEditLog.getId()
            : sequence.incrementAndGet();
        LocalDateTime createdAt = meetingRecordEditLog.getCreatedAt() != null
            ? meetingRecordEditLog.getCreatedAt()
            : LocalDateTime.now();

        MeetingRecordEditLog saved = MeetingRecordEditLog.builder()
            .id(id)
            .meetingRecordId(meetingRecordEditLog.getMeetingRecordId())
            .teamId(meetingRecordEditLog.getTeamId())
            .editorId(meetingRecordEditLog.getEditorId())
            .reason(meetingRecordEditLog.getReason())
            .createdAt(createdAt)
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Page<MeetingRecordEditLog> findAllByMeetingRecordId(Long meetingRecordId, Pageable pageable) {
        return page(store.values().stream()
            .filter(log -> log.getMeetingRecordId().equals(meetingRecordId))
            .sorted(comparator(pageable))
            .toList(), pageable);
    }

    @Override
    public Page<MeetingRecordEditLog> findAllByTeamIdIn(
        List<Long> teamIds,
        Long meetingRecordId,
        Pageable pageable
    ) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return page(store.values().stream()
            .filter(log -> teamIds.contains(log.getTeamId()))
            .filter(log -> meetingRecordId == null || meetingRecordId.equals(log.getMeetingRecordId()))
            .sorted(comparator(pageable))
            .toList(), pageable);
    }

    @Override
    public Map<Long, Long> countByTeamIdInUntil(List<Long> teamIds, LocalDateTime until) {
        if (teamIds == null || teamIds.isEmpty() || until == null) {
            return Map.of();
        }
        return store.values().stream()
            .filter(log -> teamIds.contains(log.getTeamId()))
            .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isAfter(until))
            .collect(Collectors.groupingBy(
                MeetingRecordEditLog::getTeamId, Collectors.counting()));
    }

    @Override
    public void deleteAllByMeetingRecordId(Long meetingRecordId) {
        store.values().removeIf(log -> log.getMeetingRecordId().equals(meetingRecordId));
    }

    public List<MeetingRecordEditLog> findAll() {
        return store.values().stream()
            .sorted(Comparator.comparing(MeetingRecordEditLog::getId))
            .toList();
    }

    private Page<MeetingRecordEditLog> page(List<MeetingRecordEditLog> logs, Pageable pageable) {
        int from = (int) Math.min(pageable.getOffset(), logs.size());
        int to = Math.min(from + pageable.getPageSize(), logs.size());
        return new PageImpl<>(logs.subList(from, to), pageable, logs.size());
    }

    // 실제 리포지토리는 Pageable의 정렬을 그대로 쓰므로, Fake도 정렬을 무시하지 않는다.
    private Comparator<MeetingRecordEditLog> comparator(Pageable pageable) {
        Comparator<MeetingRecordEditLog> comparator = null;
        for (Sort.Order order : pageable.getSort()) {
            Comparator<MeetingRecordEditLog> current = "createdAt".equals(order.getProperty())
                ? Comparator.comparing(
                    MeetingRecordEditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(MeetingRecordEditLog::getId);
            if (order.isDescending()) {
                current = current.reversed();
            }
            comparator = comparator == null ? current : comparator.thenComparing(current);
        }
        return comparator == null
            ? Comparator.comparing(MeetingRecordEditLog::getId)
            : comparator;
    }
}
