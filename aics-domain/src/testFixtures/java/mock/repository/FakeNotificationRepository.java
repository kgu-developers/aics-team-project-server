package mock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationRepository;

public class FakeNotificationRepository implements NotificationRepository {

    private final Map<Long, Notification> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Notification save(Notification notification) {
        Long id = notification.getId() != null ? notification.getId() : sequence.incrementAndGet();

        Notification saved = Notification.builder()
            .id(id)
            .userId(notification.getUserId())
            .type(notification.getType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .link(notification.getLink())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        store.put(id, saved);
        return saved;
    }

    /** 테스트에서 브로드캐스트 결과를 확인하기 위한 헬퍼. 실제 Repository 인터페이스엔 없다. */
    public List<Notification> findAll() {
        return store.values().stream().toList();
    }
}
