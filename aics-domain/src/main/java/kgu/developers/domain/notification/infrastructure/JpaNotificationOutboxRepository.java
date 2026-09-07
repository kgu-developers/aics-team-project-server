package kgu.developers.domain.notification.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface JpaNotificationOutboxRepository extends JpaRepository<NotificationOutboxJpaEntity, Long> {

    /**
     * 다음 시도 시각이 지났고 재시도 횟수가 남은 건. 첫 발송 지연도 백오프도 nextAttemptAt 한 컬럼에 들어 있다.
     * 이전 버전에서 만들어져 nextAttemptAt 이 비어 있는 행은 바로 대상이 된다.
     */
    @Query("SELECT o.id FROM NotificationOutboxJpaEntity o WHERE o.retryCount < :maxRetries AND (o.nextAttemptAt IS NULL OR o.nextAttemptAt <= :now) ORDER BY o.createdAt ASC")
    List<Long> findDueOutboxIds(
        @Param("maxRetries") int maxRetries,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    @Lock(PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT o FROM NotificationOutboxJpaEntity o WHERE o.id = :id")
    Optional<NotificationOutboxJpaEntity> lockById(@Param("id") Long id);
}
