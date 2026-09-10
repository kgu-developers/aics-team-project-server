package editlock.infrastructure;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import kgu.developers.domain.editlock.domain.EditLock;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import kgu.developers.domain.editlock.infrastructure.EditLockJpaEntity;
import kgu.developers.domain.editlock.infrastructure.EditLockRepositoryImpl;
import kgu.developers.domain.editlock.infrastructure.JpaEditLockRepository;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:editlock-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
    EditLockRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EditLockPersistenceIntegrationTest {

    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.editlock.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.editlock.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private EditLockRepositoryImpl editLockRepository;

    @Autowired
    private JpaEditLockRepository jpaEditLockRepository;

    @Test
    @DisplayName("EditLock 최초 저장 시 BaseTimeEntity의 createdAt과 updatedAt이 정상 생성된다")
    void saveEditLock_PopulatesCreatedAtAndUpdatedAt() {
        EditLock lock = EditLock.create(
                EditLockTargetType.MEETING_RECORD, 2L, "DEFAULT", "202411111", LocalDateTime.now());

        EditLock saved = editLockRepository.save(lock);

        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getCreatedAt()).isNotNull();
        Assertions.assertThat(saved.getUpdatedAt()).isNotNull();

        EditLockJpaEntity entity = jpaEditLockRepository.findById(saved.getId()).orElseThrow();
        Assertions.assertThat(entity.getCreatedAt()).isNotNull();
        Assertions.assertThat(entity.getUpdatedAt()).isNotNull();
        Assertions.assertThat(entity.getLockedBy()).isEqualTo("202411111");
    }

    @Test
    @DisplayName("기존 EditLock 갱신(renew) 시 createdAt이 유지되어 NOT NULL 위반(409) 없이 정상 저장된다")
    void renewAndSaveEditLock_PreservesCreatedAtAndUpdatesSuccessfully() {
        EditLock lock = EditLock.create(
                EditLockTargetType.MEETING_RECORD, 2L, "DEFAULT", "202411111", LocalDateTime.now());
        EditLock saved = editLockRepository.save(lock);
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        Assertions.assertThat(originalCreatedAt).isNotNull();

        // 갱신 (하트비트/인수)
        saved.renew("202422222", LocalDateTime.now().plusSeconds(30));

        Assertions.assertThatCode(() -> {
            EditLock renewed = editLockRepository.save(saved);
            Assertions.assertThat(renewed.getId()).isEqualTo(saved.getId());
            Assertions.assertThat(renewed.getLockedBy()).isEqualTo("202422222");
            Assertions.assertThat(renewed.getCreatedAt()).isEqualTo(originalCreatedAt);
        }).doesNotThrowAnyException();

        EditLockJpaEntity entity = jpaEditLockRepository.findById(saved.getId()).orElseThrow();
        Assertions.assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt);
        Assertions.assertThat(entity.getLockedBy()).isEqualTo("202422222");
    }

    @Test
    @DisplayName("동일 대상에 대해 중복 신규 저장 시 uk_edit_lock_target 유니크 제약에 의해 EditLockConflictException이 발생한다")
    void saveDuplicateLock_ThrowsEditLockConflictException() {
        EditLock lock1 = EditLock.create(
                EditLockTargetType.MEETING_RECORD, 2L, "DEFAULT", "202411111", LocalDateTime.now());
        editLockRepository.save(lock1);

        EditLock lock2 = EditLock.create(
                EditLockTargetType.MEETING_RECORD, 2L, "DEFAULT", "202422222", LocalDateTime.now());

        Assertions.assertThatThrownBy(() -> editLockRepository.save(lock2))
                .isInstanceOf(EditLockConflictException.class);
    }
}
