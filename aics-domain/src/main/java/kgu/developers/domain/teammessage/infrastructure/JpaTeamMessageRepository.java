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

    Page<TeamMessageJpaEntity> findByThreadIdAndRelatedTypeAndRelatedId(Long threadId, TeamMessageRelatedType relatedType, Long relatedId, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdIn(List<Long> threadIds, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdInAndRelatedType(List<Long> threadIds, TeamMessageRelatedType relatedType, Pageable pageable);

    List<TeamMessageJpaEntity> findAllByBroadcastId(String broadcastId);

    // 일괄 발송(broadcastId)은 항상 전체 동기화로 읽음 처리되므로(markAsRead가 같은 broadcastId 메시지를
    // 전부 함께 처리) 한 그룹은 "전부 읽음" 또는 "전부 안읽음" 둘 중 하나뿐이다. 그래서 묶음 단위(그룹 수)로
    // 안읽음 개수를 세도 메시지 단위로 센 것과 모순되지 않는다.
    @Query("""
        SELECT COUNT(DISTINCT COALESCE(message.broadcastId, CAST(message.id AS string)))
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
