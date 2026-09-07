package kgu.developers.domain.notification.application.command;

import java.util.List;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import kgu.developers.domain.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationOutboxService {

    private final NotificationOutboxRepository notificationOutboxRepository;

    public void createNotificationOutbox(
        String userId,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {
        NotificationOutbox outbox = NotificationOutbox.create(
            userId, type, sourceId, title, message, link
        );
        notificationOutboxRepository.save(outbox);
    }

    public void broadcastNotificationOutbox(
        List<String> userIds,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {
        userIds.forEach(userId -> createNotificationOutbox(userId, type, sourceId, title, message, link));
    }
}