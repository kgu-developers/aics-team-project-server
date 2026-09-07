package kgu.developers.domain.notification.application.command;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxWorker notificationOutboxWorker;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 50;

    // 트랜잭션은 건별로 NotificationOutboxWorker 가 연다. 여기에 @Transactional 을 붙이면 실패 표시가 같이 롤백된다.
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void processOutbox() {
        // 첫 발송 지연과 재시도 간격은 NotificationOutbox 가 nextAttemptAt 에 박아둔다.
        List<Long> dueIds = notificationOutboxRepository
            .findDueOutboxIds(MAX_RETRIES, LocalDateTime.now(), BATCH_SIZE);

        if (dueIds.isEmpty()) {
            return;
        }

        log.info("알림 아웃박스 {}건 처리 시작", dueIds.size());

        for (Long id : dueIds) {
            try {
                notificationOutboxWorker.process(id);
            } catch (Exception e) {
                log.error("알림 아웃박스 {}번 처리 실패: {}", id, e.getMessage(), e);
                notificationOutboxWorker.markFailed(id, MAX_RETRIES, e.getMessage());
            }
        }
    }
}
