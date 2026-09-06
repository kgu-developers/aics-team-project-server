package kgu.developers.domain.notification.application.command;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import kgu.developers.domain.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationRepository notificationRepository;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    @Transactional
    public void processOutbox() {
        List<NotificationOutbox> pendingOutboxes = notificationOutboxRepository
            .findPendingOrRetryableOutboxesBefore(MAX_RETRIES, LocalDateTime.now().minusSeconds(10))
            .stream()
            .limit(BATCH_SIZE)
            .toList();

        if (pendingOutboxes.isEmpty()) {
            return;
        }

        log.info("Processing {} notification outbox entries", pendingOutboxes.size());

        for (NotificationOutbox outbox : pendingOutboxes) {
            try {
                processSingleOutbox(outbox);
            } catch (Exception e) {
                log.error("Failed to process outbox entry {}: {}", outbox.getId(), e.getMessage(), e);
                // 다음 항목으로 계속 진행
            }
        }
    }

    private void processSingleOutbox(NotificationOutbox outbox) {
        try {
            // 실제 알림 생성
            Notification notification = Notification.create(
                outbox.getUserId(),
                outbox.getType(),
                outbox.getSourceId(),
                outbox.getTitle(),
                outbox.getMessage(),
                outbox.getLink()
            );
            notificationRepository.save(notification);

            // 아웃박스 항목 삭제
            notificationOutboxRepository.delete(outbox);

            log.debug("Successfully processed outbox entry {}", outbox.getId());
        } catch (Exception e) {
            // 실패 처리
            outbox.markAsFailed(e.getMessage());
            notificationOutboxRepository.save(outbox);

            if (!outbox.canRetry(MAX_RETRIES)) {
                log.error("Outbox entry {} exceeded max retries. Final error: {}", 
                    outbox.getId(), e.getMessage());
            } else {
                log.warn("Outbox entry {} failed (attempt {}/{}): {}", 
                    outbox.getId(), outbox.getRetryCount(), MAX_RETRIES, e.getMessage());
            }
            throw e; // 트랜잭션 롤백을 위해 예외 다시 던지기
        }
    }
}