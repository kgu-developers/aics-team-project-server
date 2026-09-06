package kgu.developers.domain.notification.domain;

import java.util.List;

public interface NotificationOutboxRepository {

    NotificationOutbox save(NotificationOutbox outbox);

    List<NotificationOutbox> findPendingOrRetryableOutboxes(int maxRetries);

    List<NotificationOutbox> findPendingOrRetryableOutboxesBefore(int maxRetries, java.time.LocalDateTime before);

    long countPendingOrRetryableOutboxes(int maxRetries);

    void delete(NotificationOutbox outbox);
}