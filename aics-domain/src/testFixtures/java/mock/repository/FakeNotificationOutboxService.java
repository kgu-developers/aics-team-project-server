package mock.repository;

import java.util.ArrayList;
import java.util.List;
import kgu.developers.domain.notification.application.command.NotificationOutboxService;
import kgu.developers.domain.notification.domain.NotificationType;

public class FakeNotificationOutboxService extends NotificationOutboxService {

    private final List<OutboxEntry> outboxEntries = new ArrayList<>();

    public FakeNotificationOutboxService() {
        super(null);
    }

    @Override
    public void createNotificationOutbox(
        String userId,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {
        outboxEntries.add(new OutboxEntry(userId, type, sourceId, title, message, link));
    }

    public List<OutboxEntry> getOutboxEntries() {
        return new ArrayList<>(outboxEntries);
    }

    public void clear() {
        outboxEntries.clear();
    }

    public record OutboxEntry(
        String userId,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {}
}