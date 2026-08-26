package kgu.developers.domain.teammessage.infrastructure;

import java.util.List;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTeamMessageRepository extends JpaRepository<TeamMessageJpaEntity, Long> {

    Page<TeamMessageJpaEntity> findByThreadId(Long threadId, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdIn(List<Long> threadIds, Pageable pageable);

    List<TeamMessageJpaEntity> findAllByThreadId(Long threadId);

    List<TeamMessageJpaEntity> findAllByThreadIdIn(List<Long> threadIds);
}
