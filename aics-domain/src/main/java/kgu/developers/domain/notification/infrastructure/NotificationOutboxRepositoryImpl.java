package kgu.developers.domain.notification.infrastructure;

import java.util.List;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
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
    public List<NotificationOutbox> findPendingOrRetryableOutboxes(int maxRetries) {
        return jpaNotificationOutboxRepository.findPendingOrRetryableOutboxes(maxRetries).stream()
            .map(NotificationOutboxJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<NotificationOutbox> findPendingOrRetryableOutboxesBefore(int maxRetries, java.time.LocalDateTime before) {
        return jpaNotificationOutboxRepository.findPendingOrRetryableOutboxesBefore(maxRetries, before).stream()
            .map(NotificationOutboxJpaEntity::toDomain)
            .toList();
    }

    @Override
    public long countPendingOrRetryableOutboxes(int maxRetries) {
        return jpaNotificationOutboxRepository.countPendingOrRetryableOutboxes(maxRetries);
    }

    @Override
    public void delete(NotificationOutbox outbox) {
        jpaNotificationOutboxRepository.deleteById(outbox.getId());
    }
}