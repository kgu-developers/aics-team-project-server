package kgu.developers.domain.notification.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaNotificationOutboxRepository extends JpaRepository<NotificationOutboxJpaEntity, Long> {

    @Query("SELECT o FROM NotificationOutboxJpaEntity o WHERE o.status = 'PENDING' OR (o.status = 'FAILED' AND o.retryCount < :maxRetries) ORDER BY o.createdAt ASC")
    List<NotificationOutboxJpaEntity> findPendingOrRetryableOutboxes(@Param("maxRetries") int maxRetries);

    @Query("SELECT o FROM NotificationOutboxJpaEntity o WHERE o.status = 'PENDING' OR (o.status = 'FAILED' AND o.retryCount < :maxRetries) AND o.createdAt < :before ORDER BY o.createdAt ASC")
    List<NotificationOutboxJpaEntity> findPendingOrRetryableOutboxesBefore(@Param("maxRetries") int maxRetries, @Param("before") LocalDateTime before);

    @Query("SELECT COUNT(o) FROM NotificationOutboxJpaEntity o WHERE o.status = 'PENDING' OR (o.status = 'FAILED' AND o.retryCount < :maxRetries)")
    long countPendingOrRetryableOutboxes(@Param("maxRetries") int maxRetries);
}