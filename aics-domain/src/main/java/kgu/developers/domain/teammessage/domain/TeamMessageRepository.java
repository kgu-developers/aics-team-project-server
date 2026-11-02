package kgu.developers.domain.teammessage.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamMessageRepository {

    TeamMessage save(TeamMessage teamMessage);

    Optional<TeamMessage> findById(Long id);

    Page<TeamMessage> findByThreadId(Long threadId, Pageable pageable);

    Page<TeamMessage> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable);

    Page<TeamMessage> findByThreadIdAndRelatedTypeAndRelatedId(Long threadId, TeamMessageRelatedType relatedType, Long relatedId, Pageable pageable);

    Page<TeamMessage> findByThreadIdIn(
        List<Long> threadIds, TeamMessageRelatedType relatedType, List<String> senderIds, Pageable pageable);

    List<TeamMessage> findAllByBroadcastId(String broadcastId);

    List<String> findDistinctSenderIdsByThreadIdIn(List<Long> threadIds);
}
