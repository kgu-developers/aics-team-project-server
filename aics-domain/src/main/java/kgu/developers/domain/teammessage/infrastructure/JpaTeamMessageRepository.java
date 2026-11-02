package kgu.developers.domain.teammessage.infrastructure;

import java.util.List;
import java.time.LocalDateTime;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTeamMessageRepository extends JpaRepository<TeamMessageJpaEntity, Long> {

    // shortcut: 현재 계정 역할로 집계한다. 역할 변경 전 이력까지 고정해야 하면 메시지에 발신 역할을 저장한다.
    @Query("""
        select message.relatedId as relatedId,
               sum(case when sender.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.ADMIN then 1 else 0 end) as feedbackCount,
               max(case when sender.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.ADMIN then message.createdAt else null end) as lastFeedbackAt,
               max(case when sender.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.USER then message.createdAt else null end) as lastStudentMessageAt
        from TeamMessageJpaEntity message
        join TeamThreadJpaEntity thread on thread.id = message.threadId
        join UserJpaEntity sender on sender.studentNumber = message.senderId
        where thread.teamId in :teamIds
          and message.relatedType = :relatedType
          and message.relatedId in :relatedIds
          and message.deletedAt is null
          and thread.deletedAt is null
        group by message.relatedId
        """)
    List<DocumentFeedbackProjection> summarizeFeedback(
        @Param("teamIds") List<Long> teamIds,
        @Param("relatedType") TeamMessageRelatedType relatedType,
        @Param("relatedIds") List<Long> relatedIds
    );

    interface DocumentFeedbackProjection {
        Long getRelatedId();
        long getFeedbackCount();
        LocalDateTime getLastFeedbackAt();
        LocalDateTime getLastStudentMessageAt();
    }

    Page<TeamMessageJpaEntity> findByThreadId(Long threadId, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdAndRelatedTypeAndRelatedId(Long threadId, TeamMessageRelatedType relatedType, Long relatedId, Pageable pageable);

    @Query(value = """
        SELECT message FROM TeamMessageJpaEntity message
        WHERE message.threadId IN :threadIds
          AND (:relatedType IS NULL OR message.relatedType = :relatedType)
          AND (:senderIds IS NULL OR message.senderId IN :senderIds)
        """,
        countQuery = """
        SELECT COUNT(message) FROM TeamMessageJpaEntity message
        WHERE message.threadId IN :threadIds
          AND (:relatedType IS NULL OR message.relatedType = :relatedType)
          AND (:senderIds IS NULL OR message.senderId IN :senderIds)
        """)
    Page<TeamMessageJpaEntity> findByThreadIdIn(
        @Param("threadIds") List<Long> threadIds,
        @Param("relatedType") TeamMessageRelatedType relatedType,
        @Param("senderIds") List<String> senderIds,
        Pageable pageable
    );

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

    @Query("""
        SELECT DISTINCT message.senderId FROM TeamMessageJpaEntity message
        WHERE message.threadId IN :threadIds AND message.senderId IS NOT NULL
        """)
    List<String> findDistinctSenderIdsByThreadIdIn(@Param("threadIds") List<Long> threadIds);
}
