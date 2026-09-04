package editlock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import kgu.developers.domain.editlock.infrastructure.EditLockJpaEntity;
import kgu.developers.domain.editlock.infrastructure.EditLockRepositoryImpl;
import kgu.developers.domain.editlock.infrastructure.JpaEditLockRepository;

@ExtendWith(MockitoExtension.class)
class EditLockRepositoryImplTest {

    @Mock
    private JpaEditLockRepository jpaEditLockRepository;

    @InjectMocks
    private EditLockRepositoryImpl editLockRepositoryImpl;

    private EditLock editLock() {
        return EditLock.create(EditLockTargetType.PRESENTATION_CONTENT, 1L, "202412345", LocalDateTime.now());
    }

    @Test
    @DisplayName("save는 정상적으로 저장되면 도메인 객체를 반환한다")
    void save_Success() {
        EditLockJpaEntity saved = EditLockJpaEntity.builder()
                .id(1L)
                .targetType(EditLockTargetType.PRESENTATION_CONTENT)
                .targetId(1L)
                .lockedBy("202412345")
                .lockedAt(LocalDateTime.now())
                .version(0L)
                .build();
        given(jpaEditLockRepository.saveAndFlush(any())).willReturn(saved);

        EditLock result = editLockRepositoryImpl.save(editLock());

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("동시에 갱신/인수를 시도해 낙관적 락 충돌이 나면 EditLockConflictException으로 변환한다")
    void save_OptimisticLockConflict_TranslatesToEditLockConflictException() {
        given(jpaEditLockRepository.saveAndFlush(any()))
                .willThrow(new ObjectOptimisticLockingFailureException(EditLockJpaEntity.class, 1L));

        assertThatThrownBy(() -> editLockRepositoryImpl.save(editLock()))
                .isInstanceOf(EditLockConflictException.class);
    }

    @Test
    @DisplayName("신규 획득 시 유니크 제약 위반이 나면 EditLockConflictException으로 변환한다")
    void save_UniqueConstraintViolation_TranslatesToEditLockConflictException() {
        given(jpaEditLockRepository.saveAndFlush(any()))
                .willThrow(new DataIntegrityViolationException("uk_edit_lock_target"));

        assertThatThrownBy(() -> editLockRepositoryImpl.save(editLock()))
                .isInstanceOf(EditLockConflictException.class);
    }
}
