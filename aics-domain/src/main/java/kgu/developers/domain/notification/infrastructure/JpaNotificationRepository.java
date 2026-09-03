package kgu.developers.domain.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {
}
