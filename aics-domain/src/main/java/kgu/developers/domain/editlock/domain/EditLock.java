package kgu.developers.domain.editlock.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EditLock {

    // 프론트가 편집 중 30초~1분 주기로 하트비트를 재호출하는 걸 전제로, 몇 번 놓쳐도 바로
    // 만료 처리되지 않게 넉넉히 잡은 값. 별도 정리 배치 없이 조회 시점에 이 값으로 계산한다.
    private static final long TTL_MINUTES = 2;

    private Long id;
    private EditLockTargetType targetType;
    private Long targetId;
    private String lockedBy;
    private LocalDateTime lockedAt;

    public static EditLock create(EditLockTargetType targetType, Long targetId, String lockedBy, LocalDateTime lockedAt) {
        return EditLock.builder()
            .targetType(targetType)
            .targetId(targetId)
            .lockedBy(lockedBy)
            .lockedAt(lockedAt)
            .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return lockedAt.plusMinutes(TTL_MINUTES).isBefore(now);
    }

    public boolean isOwnedBy(String userId) {
        return lockedBy.equals(userId);
    }

    public void renew(String lockedBy, LocalDateTime lockedAt) {
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
    }
}
