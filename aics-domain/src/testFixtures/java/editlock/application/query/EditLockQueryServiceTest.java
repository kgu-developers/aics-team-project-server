package editlock.application.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import mock.repository.FakeEditLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EditLockQueryServiceTest {

    private static final EditLockTargetType TARGET_TYPE = EditLockTargetType.PRESENTATION_CONTENT;
    private static final Long TARGET_ID = 1L;

    private FakeEditLockRepository fakeEditLockRepository;
    private EditLockQueryService queryService;

    @BeforeEach
    void init() {
        fakeEditLockRepository = new FakeEditLockRepository();
        queryService = new EditLockQueryService(fakeEditLockRepository);
    }

    @Test
    @DisplayName("getActiveLock은 잠금이 없으면 빈 값을 반환한다")
    void getActiveLock_NotExists_ReturnsEmpty() {
        // when
        Optional<EditLock> result = queryService.getActiveLock(TARGET_TYPE, TARGET_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getActiveLock은 살아있는 잠금이 있으면 반환한다")
    void getActiveLock_Active_ReturnsIt() {
        // given
        fakeEditLockRepository.save(EditLock.create(TARGET_TYPE, TARGET_ID, "202412345", LocalDateTime.now()));

        // when
        Optional<EditLock> result = queryService.getActiveLock(TARGET_TYPE, TARGET_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getLockedBy()).isEqualTo("202412345");
    }

    @Test
    @DisplayName("getActiveLock은 만료된 잠금은 없는 것으로 취급한다")
    void getActiveLock_Expired_ReturnsEmpty() {
        // given
        fakeEditLockRepository.save(
            EditLock.create(TARGET_TYPE, TARGET_ID, "202412345", LocalDateTime.now().minusMinutes(10))
        );

        // when
        Optional<EditLock> result = queryService.getActiveLock(TARGET_TYPE, TARGET_ID);

        // then
        assertThat(result).isEmpty();
    }
}
