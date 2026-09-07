package kgu.developers.domain.notification.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    public List<Long> findDueOutboxIds(int maxRetries, LocalDateTime before, int limit) {
        return jpaNotificationOutboxRepository.findDueOutboxIds(maxRetries, before, PageRequest.ofSize(limit));
    }

    @Override
    public Optional<NotificationOutbox> lockById(Long id) {
        return jpaNotificationOutboxRepository.lockById(id).map(NotificationOutboxJpaEntity::toDomain);
    }

    @Override
    public void delete(NotificationOutbox outbox) {
        jpaNotificationOutboxRepository.deleteById(outbox.getId());
    }
}
