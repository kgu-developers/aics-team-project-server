package kgu.developers.domain.teammessage.infrastructure;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceipt;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamMessageReadReceiptRepositoryImpl implements TeamMessageReadReceiptRepository {

    private final JpaTeamMessageReadReceiptRepository jpaTeamMessageReadReceiptRepository;

    @Override
    public TeamMessageReadReceipt save(TeamMessageReadReceipt readReceipt) {
        return jpaTeamMessageReadReceiptRepository.save(TeamMessageReadReceiptJpaEntity.toEntity(readReceipt)).toDomain();
    }

    @Override
    public boolean existsByMessageIdAndUserId(Long messageId, String userId) {
        return jpaTeamMessageReadReceiptRepository.existsByMessageIdAndUserId(messageId, userId);
    }

    @Override
    public Set<Long> findReadMessageIds(String userId, List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Set.of();
        }
        return jpaTeamMessageReadReceiptRepository.findAllByUserIdAndMessageIdIn(userId, messageIds).stream()
            .map(TeamMessageReadReceiptJpaEntity::getMessageId)
            .collect(Collectors.toSet());
    }
}
