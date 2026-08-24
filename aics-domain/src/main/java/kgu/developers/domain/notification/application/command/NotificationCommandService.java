package kgu.developers.domain.notification.application.command;

import java.util.List;
import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationRepository;
import kgu.developers.domain.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;

    public void createNotification(String userId, NotificationType type, Long sourceId, String title, String message, String link) {
        notificationRepository.save(Notification.create(userId, type, sourceId, title, message, link));
    }

    public void broadcast(List<String> userIds, NotificationType type, Long sourceId, String title, String message, String link) {
        userIds.forEach(userId -> createNotification(userId, type, sourceId, title, message, link));
    }
}
