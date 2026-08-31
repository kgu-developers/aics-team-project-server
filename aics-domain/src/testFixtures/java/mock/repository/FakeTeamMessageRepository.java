package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.domain.TeamMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class FakeTeamMessageRepository implements TeamMessageRepository {

    private final Map<Long, TeamMessage> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public TeamMessage save(TeamMessage teamMessage) {
        Long id = teamMessage.getId() != null ? teamMessage.getId() : sequence.incrementAndGet();
        TeamMessage saved = TeamMessage.builder()
            .id(id)
            .threadId(teamMessage.getThreadId())
            .senderId(teamMessage.getSenderId())
            .message(teamMessage.getMessage())
            .relatedType(teamMessage.getRelatedType())
            .relatedId(teamMessage.getRelatedId())
            .important(teamMessage.isImportant())
            .createdAt(teamMessage.getCreatedAt() != null ? teamMessage.getCreatedAt() : LocalDateTime.now())
            .build();
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<TeamMessage> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Page<TeamMessage> findByThreadId(Long threadId, Pageable pageable) {
        List<TeamMessage> filtered = store.values().stream()
            .filter(message -> message.getThreadId().equals(threadId))
            .toList();
        return toPage(applySort(filtered, pageable.getSort()), pageable);
    }

    @Override
    public Page<TeamMessage> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable) {
        List<TeamMessage> filtered = store.values().stream()
            .filter(message -> message.getThreadId().equals(threadId) && message.getRelatedType() == relatedType)
            .toList();
        return toPage(applySort(filtered, pageable.getSort()), pageable);
    }

    @Override
    public Page<TeamMessage> findByThreadIdIn(List<Long> threadIds, Pageable pageable) {
        List<TeamMessage> filtered = store.values().stream()
            .filter(message -> threadIds.contains(message.getThreadId()))
            .toList();
        return toPage(applySort(filtered, pageable.getSort()), pageable);
    }

    // JPA 쪽은 Spring Data가 Pageable.getSort()를 자동 반영하므로, Fake도 동일하게 정렬을 적용해서 동작을 맞춘다.
    // 정렬을 안 주면(unsorted) 컨트롤러의 @PageableDefault(id desc) 기본값이 항상 채워져서 들어온다.
    private List<TeamMessage> applySort(List<TeamMessage> messages, Sort sort) {
        if (sort.isUnsorted()) {
            return messages.stream()
                .sorted(Comparator.comparing(TeamMessage::getId).reversed())
                .toList();
        }
        Comparator<TeamMessage> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<TeamMessage> propertyComparator = comparatorFor(order.getProperty());
            if (order.isDescending()) {
                propertyComparator = propertyComparator.reversed();
            }
            comparator = (comparator == null) ? propertyComparator : comparator.thenComparing(propertyComparator);
        }
        return messages.stream().sorted(comparator).toList();
    }

    private Comparator<TeamMessage> comparatorFor(String property) {
        if ("createdAt".equals(property)) {
            return Comparator.comparing(TeamMessage::getCreatedAt);
        }
        return Comparator.comparing(TeamMessage::getId);
    }

    List<Long> findMessageIdsByThreadIdIn(List<Long> threadIds) {
        return store.values().stream()
            .filter(message -> threadIds.contains(message.getThreadId()))
            .map(TeamMessage::getId)
            .toList();
    }

    private Page<TeamMessage> toPage(List<TeamMessage> filtered, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start > filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }
}
