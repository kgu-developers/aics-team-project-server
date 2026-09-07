package section.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.infrastructure.SectionRepositoryImpl;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

/**
 * 배포 DB에는 응답에서 쓰지 않는 section.name 컬럼이 NOT NULL 로 남아 있고,
 * 길이는 varchar(64)에서 varchar(200)으로 넓혔다.
 * 그 스키마를 실제 PostgreSQL(Testcontainers)에 재현해 신규 분반 생성이 되는지 검증한다.
 */
@DataJpaTest
@Testcontainers
@Import(SectionRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(propagation = NOT_SUPPORTED)
class SectionRepositoryJpaIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    // 컨테이너째 버려지므로 drop DDL은 필요 없다
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
  }

  @SpringBootConfiguration
  @EntityScan("kgu.developers")
  @EnableJpaRepositories("kgu.developers")
  static class TestConfig {
  }

  @Autowired
  private SectionRepositoryImpl repository;

  @Autowired
  private EntityManager entityManager;

  private TransactionTemplate tx;
  private Long courseId;

  @Autowired
  void setTransactionTemplate(PlatformTransactionManager transactionManager) {
    this.tx = new TransactionTemplate(transactionManager);
  }

  @BeforeEach
  void setUp() {
    tx.executeWithoutResult(status -> {
      entityManager.createQuery("delete from SectionJpaEntity").executeUpdate();
      entityManager.createQuery("delete from CourseJpaEntity").executeUpdate();
      entityManager.createQuery("delete from UserJpaEntity").executeUpdate();

      // 배포 DB의 name은 varchar(64) -> varchar(200)으로 넓혔고, NOT NULL은 유지된다.
      // 엔티티가 그대로 varchar(200) NOT NULL을 만들므로 별도 alter 없이 배포 스키마와 같다.

      UserJpaEntity professor = UserJpaEntity.builder()
          .studentNumber("202000001")
          .email("professor@kgu.ac.kr")
          .name("교수")
          .password("password")
          .globalRole(UserGlobalRole.ADMIN)
          .phone("01000000000")
          .build();
      entityManager.persist(professor);

      CourseJpaEntity course = CourseJpaEntity.builder()
          .name("소프트웨어공학")
          .year(2026)
          .semester(SemesterType.SPRING)
          .status(StatusType.ACTIVE)
          .build();
      entityManager.persist(course);
      entityManager.flush();

      courseId = course.getId();
    });
  }

  @Test
  @DisplayName("name이 NOT NULL로 남아 있는 기존 스키마에서도 신규 분반 생성이 성공한다")
  void createsSectionOnLegacySchema() {
    Section section = Section.create("202000001", courseId, "1154", "월123", 40,
        LocalDateTime.of(2026, 3, 2, 9, 0), LocalDateTime.of(2026, 6, 20, 18, 0));

    Section saved = tx.execute(status -> repository.save(section));

    assertThat(saved.getId()).isNotNull();
    assertThat(legacyName(saved.getId())).isEqualTo("월123/1154");
  }

  @Test
  @DisplayName("수업시간을 수정하면 레거시 name 컬럼도 함께 갱신된다")
  void refreshesLegacyNameOnUpdate() {
    Section section = Section.create("202000001", courseId, "1154", "월123", 40, null, null);
    Section saved = tx.execute(status -> repository.save(section));

    saved.updateClassTime("화456");
    tx.executeWithoutResult(status -> repository.save(saved));

    assertThat(legacyName(saved.getId())).isEqualTo("화456/1154");
  }

  @Test
  @DisplayName("레거시 varchar(64) 스키마에서는 최대 길이 name이 넘치고, varchar(200)으로 넓히면 저장된다")
  void requiresWideningForMaxLengthName() {
    // 요청에서 허용하는 최대 입력: classTime(100) + "/" + code(50) = 151자
    String classTime = "월".repeat(100);
    String code = "1".repeat(50);
    String expected = classTime + "/" + code;

    alterNameColumn("varchar(64)");
    assertThatThrownBy(() -> tx.execute(status -> repository.save(
        Section.create("202000001", courseId, code, classTime, 40, null, null))))
        .isInstanceOf(DataIntegrityViolationException.class);

    alterNameColumn("varchar(200)");
    Section saved = tx.execute(status -> repository.save(
        Section.create("202000001", courseId, code, classTime, 40, null, null)));

    assertThat(expected).hasSize(151);
    assertThat(legacyName(saved.getId())).isEqualTo(expected);
  }

  private void alterNameColumn(String type) {
    tx.executeWithoutResult(status -> entityManager
        .createNativeQuery("alter table section alter column name type " + type)
        .executeUpdate());
  }

  private String legacyName(Long sectionId) {
    return tx.execute(status -> (String) entityManager
        .createNativeQuery("select name from section where id = :id")
        .setParameter("id", sectionId)
        .getSingleResult());
  }
}
