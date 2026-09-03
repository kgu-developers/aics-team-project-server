package editlock.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import mock.repository.FakeEditLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EditLockCommandServiceTest {

    private static final EditLockTargetType TARGET_TYPE = EditLockTargetType.PRESENTATION_CONTENT;
    private static final Long TARGET_ID = 1L;

    private FakeEditLockRepository fakeEditLockRepository;
    private EditLockCommandService commandService;

    @BeforeEach
    void init() {
        fakeEditLockRepository = new FakeEditLockRepository();
        commandService = new EditLockCommandService(fakeEditLockRepository);
    }

    @Test
    @DisplayName("acquire는 잠금이 없으면 새로 획득한다")
    void acquire_NoExistingLock_CreatesNew() {
        // when
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");

        // then
        EditLock lock = fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID).orElseThrow();
        assertThat(lock.getLockedBy()).isEqualTo("202412345");
    }

    @Test
    @DisplayName("acquire는 본인 소유 잠금이면 하트비트로 갱신한다")
    void acquire_OwnLock_RenewsHeartbeat() {
        // given
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");
        LocalDateTime firstLockedAt = fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID)
            .orElseThrow().getLockedAt();

        // when
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");

        // then
        LocalDateTime renewedLockedAt = fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID)
            .orElseThrow().getLockedAt();
        assertThat(renewedLockedAt).isAfterOrEqualTo(firstLockedAt);
    }

    @Test
    @DisplayName("acquire는 타인이 살아있게 잠그고 있으면 예외를 던진다")
    void acquire_OtherActiveLock_ThrowsConflict() {
        // given
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");

        // when & then
        assertThatThrownBy(() -> commandService.acquire(TARGET_TYPE, TARGET_ID, "202499999"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("acquire는 타인의 잠금이 만료됐으면 가져온다")
    void acquire_OtherExpiredLock_TakesOver() {
        // given
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");
        Long lockId = fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID).orElseThrow().getId();
        fakeEditLockRepository.forceLockedAt(lockId, LocalDateTime.now().minusMinutes(10));

        // when
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202499999");

        // then
        EditLock lock = fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID).orElseThrow();
        assertThat(lock.getLockedBy()).isEqualTo("202499999");
    }

    @Test
    @DisplayName("release는 본인 소유 잠금을 해제한다")
    void release_OwnLock_Removes() {
        // given
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");

        // when
        commandService.release(TARGET_TYPE, TARGET_ID, "202412345");

        // then
        assertThat(fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID)).isEmpty();
    }

    @Test
    @DisplayName("release는 타인 소유 잠금이면 아무 일도 하지 않는다")
    void release_OtherLock_DoesNothing() {
        // given
        commandService.acquire(TARGET_TYPE, TARGET_ID, "202412345");

        // when
        commandService.release(TARGET_TYPE, TARGET_ID, "202499999");

        // then
        assertThat(fakeEditLockRepository.findByTargetTypeAndTargetId(TARGET_TYPE, TARGET_ID)).isPresent();
    }

    @Test
    @DisplayName("release는 잠금이 없어도 예외를 던지지 않는다")
    void release_NoLock_DoesNothing() {
        // when & then (예외가 안 나면 통과)
        commandService.release(TARGET_TYPE, TARGET_ID, "202412345");
    }
}
