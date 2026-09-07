package kgu.developers.domain.notification.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationOutbox {

    private Long id;
    private String userId;
    private NotificationType type;
    private Long sourceId;
    private String title;
    private String message;
    private String link;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime nextAttemptAt;   // 이 시각 전에는 다시 집지 않는다. 스케줄러의 유일한 시간 기준이다.
    private int retryCount;
    private String errorMessage;

    /** 만들자마자 집어가지 않는다 — 아웃박스를 기록한 트랜잭션이 커밋될 시간을 준다. */
    private static final Duration FIRST_ATTEMPT_DELAY = Duration.ofSeconds(10);

    /** 재시도 간격의 첫 칸. 실패가 쌓이면 두 배씩 벌어진다(30초 → 1분 → 2분). */
    private static final Duration RETRY_BASE_INTERVAL = Duration.ofSeconds(30);

    public static NotificationOutbox create(
        String userId,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {
        LocalDateTime now = LocalDateTime.now();
        return NotificationOutbox.builder()
            .userId(userId)
            .type(type)
            .sourceId(sourceId)
            .title(title)
            .message(message)
            .link(link)
            .status(OutboxStatus.PENDING)
            .createdAt(now)
            .nextAttemptAt(now.plus(FIRST_ATTEMPT_DELAY))
            .retryCount(0)
            .build();
    }

    /**
     * 실패를 기록하고 다음 시도 시각을 뒤로 민다. 이 값이 없으면 5초짜리 스케줄이 일시적 장애 동안
     * 재시도 횟수를 순식간에 다 태운다.
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.nextAttemptAt = LocalDateTime.now().plus(RETRY_BASE_INTERVAL.multipliedBy(1L << (retryCount - 1)));
    }

    public boolean canRetry(int maxRetries) {
        return this.status == OutboxStatus.FAILED && this.retryCount < maxRetries;
    }
}