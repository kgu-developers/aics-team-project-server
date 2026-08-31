package kgu.developers.domain.teammessage.infrastructure;

import java.util.List;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTeamMessageRepository extends JpaRepository<TeamMessageJpaEntity, Long> {

    Page<TeamMessageJpaEntity> findByThreadId(Long threadId, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdIn(List<Long> threadIds, Pageable pageable);

    @Query("""
        SELECT COUNT(message)
        FROM TeamMessageJpaEntity message
        WHERE message.threadId IN :threadIds
          AND NOT EXISTS (
              SELECT receipt.id
              FROM TeamMessageReadReceiptJpaEntity receipt
              WHERE receipt.messageId = message.id
                AND receipt.userId = :userId
          )
        """)
    long countUnreadByThreadIdIn(
        @Param("threadIds") List<Long> threadIds,
        @Param("userId") String userId
    );
}
