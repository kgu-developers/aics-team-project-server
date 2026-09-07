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

    @Query("SELECT o.id FROM NotificationOutboxJpaEntity o WHERE (o.status = 'PENDING' OR (o.status = 'FAILED' AND o.retryCount < :maxRetries)) AND o.createdAt < :before ORDER BY o.createdAt ASC")
    List<Long> findDueOutboxIds(
        @Param("maxRetries") int maxRetries,
        @Param("before") LocalDateTime before,
        Pageable pageable
    );

    @Lock(PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT o FROM NotificationOutboxJpaEntity o WHERE o.id = :id")
    Optional<NotificationOutboxJpaEntity> lockById(@Param("id") Long id);
}
