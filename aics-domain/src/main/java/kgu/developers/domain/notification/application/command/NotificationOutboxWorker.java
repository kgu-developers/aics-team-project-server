package kgu.developers.domain.notification.application.command;

import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationOutboxRepository;
import kgu.developers.domain.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 한 건이 곧 한 트랜잭션이다. 배치 전체를 한 트랜잭션으로 묶으면 한 건이 실패할 때
 * 나머지 성공분까지 같이 롤백되고, 실패 표시(retryCount 증가)도 남지 않아 영원히 같은 건을 다시 집는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void process(Long id) {
        notificationOutboxRepository.lockById(id).ifPresent(outbox -> {
            notificationRepository.save(Notification.create(
                outbox.getUserId(),
                outbox.getType(),
                outbox.getSourceId(),
                outbox.getTitle(),
                outbox.getMessage(),
                outbox.getLink()
            ));
            notificationOutboxRepository.delete(outbox);

            log.debug("알림 아웃박스 {}번 처리 완료", id);
        });
    }

    /** process 의 트랜잭션이 롤백된 뒤 새 트랜잭션에서 부른다. 같은 트랜잭션에 두면 실패 표시가 같이 롤백된다. */
    @Transactional
    public void markFailed(Long id, int maxRetries, String errorMessage) {
        notificationOutboxRepository.lockById(id).ifPresent(outbox -> {
            outbox.markAsFailed(errorMessage);
            notificationOutboxRepository.save(outbox);

            if (outbox.canRetry(maxRetries)) {
                log.warn("알림 아웃박스 {}번 실패 ({}/{}회): {}", id, outbox.getRetryCount(), maxRetries, errorMessage);
            } else {
                log.error("알림 아웃박스 {}번 최대 재시도 초과. 마지막 에러: {}", id, errorMessage);
            }
        });
    }
}
