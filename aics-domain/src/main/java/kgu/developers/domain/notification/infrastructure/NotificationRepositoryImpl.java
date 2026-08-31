package kgu.developers.domain.notification.infrastructure;

import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JpaNotificationRepository jpaNotificationRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaNotificationRepository.save(NotificationJpaEntity.toEntity(notification)).toDomain();
    }
}
