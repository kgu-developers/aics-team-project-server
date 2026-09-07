package kgu.developers.domain.notification.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository {

    NotificationOutbox save(NotificationOutbox outbox);

    /** 반환된 행은 호출 트랜잭션이 끝날 때까지 잠긴다. 다른 인스턴스는 잠긴 행을 건너뛴다. */
    List<NotificationOutbox> lockPendingOrRetryableOutboxesBefore(int maxRetries, LocalDateTime before, int limit);

    void delete(NotificationOutbox outbox);
}
