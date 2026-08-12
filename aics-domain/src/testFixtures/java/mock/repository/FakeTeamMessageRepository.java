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
            .sorted(Comparator.comparing(TeamMessage::getId).reversed())
            .toList();
        return toPage(filtered, pageable);
    }

    @Override
    public Page<TeamMessage> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable) {
        List<TeamMessage> filtered = store.values().stream()
            .filter(message -> message.getThreadId().equals(threadId) && message.getRelatedType() == relatedType)
            .sorted(Comparator.comparing(TeamMessage::getId).reversed())
            .toList();
        return toPage(filtered, pageable);
    }

    @Override
    public List<Long> findIdsByThreadId(Long threadId) {
        return store.values().stream()
            .filter(message -> message.getThreadId().equals(threadId))
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
