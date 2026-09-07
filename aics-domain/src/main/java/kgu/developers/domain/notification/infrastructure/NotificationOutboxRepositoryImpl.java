package kgu.developers.domain.notification.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepository {

    private final JpaNotificationOutboxRepository jpaNotificationOutboxRepository;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        return jpaNotificationOutboxRepository.save(NotificationOutboxJpaEntity.toEntity(outbox)).toDomain();
    }

    @Override
    public List<NotificationOutbox> lockPendingOrRetryableOutboxesBefore(int maxRetries, LocalDateTime before, int limit) {
        return jpaNotificationOutboxRepository
            .findPendingOrRetryableOutboxesBefore(maxRetries, before, PageRequest.ofSize(limit)).stream()
            .map(NotificationOutboxJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void delete(NotificationOutbox outbox) {
        jpaNotificationOutboxRepository.deleteById(outbox.getId());
    }
}
