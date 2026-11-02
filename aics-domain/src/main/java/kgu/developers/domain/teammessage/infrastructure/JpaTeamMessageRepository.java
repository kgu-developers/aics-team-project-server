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

    @Query("""
        select message.relatedId as relatedId,
               sum(case when message.senderId = :professorId then 1 else 0 end) as feedbackCount,
               max(case when message.senderId = :professorId then message.createdAt else null end) as lastFeedbackAt,
               max(case when message.senderId <> :professorId then message.createdAt else null end) as lastStudentMessageAt
        from TeamMessageJpaEntity message
        join TeamThreadJpaEntity thread on thread.id = message.threadId
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
        @Param("relatedIds") List<Long> relatedIds,
        @Param("professorId") String professorId
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

    Page<TeamMessageJpaEntity> findByThreadIdIn(List<Long> threadIds, Pageable pageable);

    Page<TeamMessageJpaEntity> findByThreadIdInAndRelatedType(List<Long> threadIds, TeamMessageRelatedType relatedType, Pageable pageable);

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
