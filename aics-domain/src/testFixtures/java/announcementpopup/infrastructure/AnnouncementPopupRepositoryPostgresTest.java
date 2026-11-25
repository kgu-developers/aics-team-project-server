package announcementpopup.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import java.util.List;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupAlreadyExistsException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupConcurrentlyModifiedException;
import kgu.developers.domain.announcementpopup.infrastructure.AnnouncementPopupJpaEntity;
import kgu.developers.domain.announcementpopup.infrastructure.AnnouncementPopupRepositoryImpl;
import kgu.developers.domain.announcementpopup.infrastructure.JpaAnnouncementPopupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 실제 Postgres에서 확인해야 하는 것들: 공지당 팝업 1개 유일 제약, {@code @Version}이 수정할 때만 오르는지,
 * 오래된 값으로 저장할 때의 충돌. Fake 저장소로는 이 동작들을 검증할 수 없다.
 * Docker가 없으면 TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD로 로컬 Postgres를 지정한다.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")  // 실 DB이므로 스키마 생성을 명시해야 한다
@AutoConfigureTestDatabase(replace = NONE)
@Import(AnnouncementPopupRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 호출마다 커밋돼야 version 증가와 충돌을 볼 수 있다
@DirtiesContext
class AnnouncementPopupRepositoryPostgresTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DB_URL");
        if (url == null) {
            PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15");
            postgres.start();
            url = postgres.getJdbcUrl();
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
        }
        String jdbcUrl = url;
        registry.add("spring.datasource.url", () -> jdbcUrl);
    }

    @Autowired
    private AnnouncementPopupRepositoryImpl repository;

    @Autowired
    private JpaAnnouncementPopupRepository jpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @SpringBootApplication
    @EntityScan(basePackageClasses = AnnouncementPopupJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = JpaAnnouncementPopupRepository.class)
    static class TestApp {
    }

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    private AnnouncementPopup save(Long announcementId, Long sectionId, String content, Long imageFileId) {
        return repository.save(AnnouncementPopup.create(announcementId, sectionId, null, content, imageFileId));
    }

    @Test
    @DisplayName("저장한 팝업은 모든 필드가 그대로 조회된다")
    void saveAndFind_RoundTrip() {
        // when
        AnnouncementPopup saved = save(10L, 1L, "안내 내용", 7L);

        // then
        AnnouncementPopup found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getAnnouncementId()).isEqualTo(10L);
        assertThat(found.getSectionId()).isEqualTo(1L);
        assertThat(found.getContent()).isEqualTo("안내 내용");
        assertThat(found.getImageFileId()).isEqualTo(7L);
        assertThat(found.isEnabled()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("제목은 200자까지 저장되고, 제목이 없으면 null로 저장·조회된다")
    void save_Title_OptionalAndMaxLength() {
        // when
        AnnouncementPopup withTitle = repository.save(
            AnnouncementPopup.create(10L, 1L, "가".repeat(200), "내용", null));
        AnnouncementPopup withoutTitle = save(11L, 1L, "내용", null);

        // then
        assertThat(repository.findById(withTitle.getId()).orElseThrow().getTitle()).hasSize(200);
        assertThat(repository.findById(withoutTitle.getId()).orElseThrow().getTitle()).isNull();
    }

    @Test
    @DisplayName("이미지가 없는 팝업은 imageFileId가 null로 저장된다")
    void save_WithoutImage_NullImageFileId() {
        // when
        AnnouncementPopup saved = save(10L, 1L, "안내 내용", null);

        // then
        assertThat(repository.findById(saved.getId()).orElseThrow().hasImage()).isFalse();
    }

    @Test
    @DisplayName("같은 공지에 두 번째 팝업을 저장하면 유일 제약 위반이 AlreadyExists로 바뀐다")
    void save_DuplicateAnnouncement_ThrowsAlreadyExists() {
        // given
        save(10L, 1L, "첫 번째", null);

        // when & then
        assertThatThrownBy(() -> save(10L, 1L, "두 번째", null))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("팝업을 수정해서 저장하면 version이 1 오른다")
    void save_Modified_IncrementsVersion() {
        // given
        AnnouncementPopup popup = save(10L, 1L, "원래 내용", null);
        assertThat(popup.getVersion()).isZero();

        // when
        popup.updateContent("수정된 내용");
        AnnouncementPopup updated = repository.save(popup);

        // then
        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(repository.findById(popup.getId()).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("노출을 껐다 켜도 수정이므로 version이 오른다(학생에게 다시 보이게 하려는 의도)")
    void save_ToggleEnabled_IncrementsVersion() {
        // given
        AnnouncementPopup popup = save(10L, 1L, "내용", null);

        // when: 저장이 돌려준 객체(새 version)로 이어서 수정한다. 옛 객체를 쓰면 충돌이 난다.
        popup.changeEnabled(false);
        AnnouncementPopup disabled = repository.save(popup);
        disabled.changeEnabled(true);
        AnnouncementPopup enabled = repository.save(disabled);

        // then
        assertThat(disabled.getVersion()).isEqualTo(1L);
        assertThat(enabled.getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 내용으로 다시 저장해도 version이 오른다(저장할 때마다 오르고 변경 여부는 따지지 않는다)")
    void save_SameContent_StillIncrementsVersion() {
        // given
        AnnouncementPopup popup = save(10L, 1L, "내용", null);

        // when
        popup.updateContent("내용");
        AnnouncementPopup resaved = repository.save(popup);

        // then: 팝업은 교수가 등록·수정할 때만 저장되므로, 같은 내용으로 저장 버튼을 눌러도 학생에게 다시 보인다.
        // 이 동작을 바꾸려면(변경이 없으면 유지) 저장 경로를 바꿔야 하고, 그때 이 테스트가 알려준다.
        assertThat(resaved.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("오래된 version으로 저장하면 동시 수정 충돌로 거부된다")
    void save_StaleVersion_ThrowsConcurrentlyModified() {
        // given: 두 요청이 같은 팝업을 읽는다
        Long id = save(10L, 1L, "원래 내용", null).getId();
        AnnouncementPopup first = repository.findById(id).orElseThrow();
        AnnouncementPopup second = repository.findById(id).orElseThrow();

        // when: 첫 요청이 먼저 저장한다
        first.updateContent("첫 번째 수정");
        repository.save(first);

        // then: 오래된 값을 가진 두 번째 요청은 덮어쓰지 못한다
        second.updateContent("두 번째 수정");
        assertThatThrownBy(() -> repository.save(second))
            .isInstanceOf(AnnouncementPopupConcurrentlyModifiedException.class);
        assertThat(repository.findById(id).orElseThrow().getContent()).isEqualTo("첫 번째 수정");
    }

    @Test
    @DisplayName("수정 저장 직후 같은 트랜잭션에서 다시 읽어도 createdAt이 유지된다(수정 응답의 createdAt이 null로 나가던 문제)")
    void save_ThenFindInSameTransaction_KeepsCreatedAt() {
        // given: 이미 커밋된 팝업을 읽어 온 상태(수정 요청이 읽어 온 도메인 객체와 같다)
        Long id = save(10L, 1L, "원래 내용", null).getId();
        AnnouncementPopup stored = repository.findById(id).orElseThrow();
        assertThat(stored.getCreatedAt()).isNotNull();
        stored.updateContent("수정된 내용");

        // when: 파사드처럼 한 트랜잭션 안에서 저장하고 바로 다시 읽는다
        AnnouncementPopup reread = new TransactionTemplate(transactionManager).execute(status -> {
            repository.save(stored);
            return repository.findById(id).orElseThrow();
        });

        // then
        assertThat(reread.getContent()).isEqualTo("수정된 내용");
        assertThat(reread.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("existsByAnnouncementId는 해당 공지에 팝업이 있을 때만 true다")
    void existsByAnnouncementId() {
        // given
        save(10L, 1L, "내용", null);

        // then
        assertThat(repository.existsByAnnouncementId(10L)).isTrue();
        assertThat(repository.existsByAnnouncementId(11L)).isFalse();
    }

    @Test
    @DisplayName("findAllBySectionId는 해당 분반의 팝업만 최신순으로, 꺼진 것도 포함해 반환한다")
    void findAllBySectionId_NewestFirst_IncludesDisabled() {
        // given
        AnnouncementPopup first = save(10L, 1L, "첫 번째", null);
        AnnouncementPopup second = save(11L, 1L, "두 번째", null);
        save(12L, 2L, "다른 분반", null);
        first.changeEnabled(false);
        repository.save(first);

        // when
        List<AnnouncementPopup> popups = repository.findAllBySectionId(1L);

        // then
        assertThat(popups).extracting(AnnouncementPopup::getId).containsExactly(second.getId(), first.getId());
        assertThat(popups.get(1).isEnabled()).isFalse();
    }
}
