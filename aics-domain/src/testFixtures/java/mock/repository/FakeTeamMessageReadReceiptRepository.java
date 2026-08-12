package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceipt;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceiptRepository;

public class FakeTeamMessageReadReceiptRepository implements TeamMessageReadReceiptRepository {

    private final Map<Long, TeamMessageReadReceipt> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public TeamMessageReadReceipt save(TeamMessageReadReceipt readReceipt) {
        Long id = readReceipt.getId() != null ? readReceipt.getId() : sequence.incrementAndGet();
        TeamMessageReadReceipt saved = TeamMessageReadReceipt.builder()
            .id(id)
            .messageId(readReceipt.getMessageId())
            .userId(readReceipt.getUserId())
            .build();
        store.put(id, saved);
        return saved;
    }

    @Override
    public boolean existsByMessageIdAndUserId(Long messageId, String userId) {
        return store.values().stream()
            .anyMatch(receipt -> receipt.getMessageId().equals(messageId) && receipt.getUserId().equals(userId));
    }

    @Override
    public Set<Long> findReadMessageIds(String userId, List<Long> messageIds) {
        return store.values().stream()
            .filter(receipt -> receipt.getUserId().equals(userId) && messageIds.contains(receipt.getMessageId()))
            .map(TeamMessageReadReceipt::getMessageId)
            .collect(Collectors.toSet());
    }
}
