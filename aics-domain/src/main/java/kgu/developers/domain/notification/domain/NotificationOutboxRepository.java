package kgu.developers.domain.notification.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {

    NotificationOutbox save(NotificationOutbox outbox);

    /** 처리 대상 후보 id. 잠그지 않으므로 다른 인스턴스도 같은 id 를 받을 수 있다. 실제 소유권은 lockById 가 정한다. */
    List<Long> findDueOutboxIds(int maxRetries, LocalDateTime before, int limit);

    /** 행을 잠근다. 다른 인스턴스가 이미 잡았거나 행이 사라졌으면 비어 있다(SKIP LOCKED). */
    Optional<NotificationOutbox> lockById(Long id);

    void delete(NotificationOutbox outbox);
}
