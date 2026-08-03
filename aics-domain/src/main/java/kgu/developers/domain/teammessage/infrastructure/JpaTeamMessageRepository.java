package kgu.developers.domain.teammessage.infrastructure;

import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTeamMessageRepository extends JpaRepository<TeamMessageJpaEntity, Long> {

    Page<TeamMessageJpaEntity> findByThreadId(Long threadId, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable);

    long countByThreadIdAndIsReadFalse(Long threadId);
}
