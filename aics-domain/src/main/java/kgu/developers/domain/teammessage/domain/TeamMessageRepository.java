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

    Page<TeamMessage> findByThreadIdIn(List<Long> threadIds, Pageable pageable);
}
