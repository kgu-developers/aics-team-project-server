package section.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.course.infrastructure.JpaCourseRepository;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionRepositoryImpl;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SectionRepositoryImpl.class)
class SuperAdminSectionAccessTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // 테스트 컨테이너가 DB째 정리하므로 종료 시 DROP은 필요 없다.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain")
    @EnableJpaRepositories("kgu.developers.domain")
    static class TestConfig {}

    @Autowired EntityManager entityManager;
    @Autowired SectionRepositoryImpl repository;
    @Autowired JpaCourseRepository courses;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void superAdminCanListReadAndLockOtherProfessorsSections() {
        UserJpaEntity professor = user("professor", UserGlobalRole.ADMIN);
        user("super", UserGlobalRole.SUPER_ADMIN);
        user("other", UserGlobalRole.ADMIN);
        CourseJpaEntity course = course("강좌");
        SectionJpaEntity section = section(professor, course, "01", false);
        section(professor, course, "02", true);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByProfessorId("super"))
            .extracting(detail -> detail.section().getId()).containsExactly(section.getId());
        assertThat(repository.findAllByCourseIdAndProfessorId(course.getId(), "super")).hasSize(1);
        assertThat(repository.existsActiveByIdAndProfessorId(section.getId(), "super")).isTrue();
        assertThat(repository.lockActiveByIdAndProfessorId(section.getId(), "super")).isTrue();
        assertThat(repository.existsActiveByIdAndProfessorId(section.getId(), "professor")).isTrue();
        assertThat(repository.lockActiveByIdAndProfessorId(section.getId(), "professor")).isTrue();
        assertThat(repository.findAllByProfessorId("other")).isEmpty();
        assertThat(repository.findAllByCourseIdAndProfessorId(course.getId(), "other")).isEmpty();
        assertThat(repository.existsActiveByIdAndProfessorId(section.getId(), "other")).isFalse();
        assertThat(repository.lockActiveByIdAndProfessorId(section.getId(), "other")).isFalse();
        assertThat(repository.findById(section.getId()).orElseThrow().section().getProfessorId())
            .isEqualTo("professor");
    }

    @Test
    void superAdminCannotAccessDeletedOrMissingSectionsOrUseDeletedAccount() {
        UserJpaEntity professor = user("professor", UserGlobalRole.ADMIN);
        UserJpaEntity superAdmin = user("super", UserGlobalRole.SUPER_ADMIN);
        CourseJpaEntity course = course("강좌");
        SectionJpaEntity active = section(professor, course, "01", false);
        SectionJpaEntity deleted = section(professor, course, "02", true);
        entityManager.flush();
        assertThat(repository.existsActiveByIdAndProfessorId(deleted.getId(), "super")).isFalse();
        assertThat(repository.lockActiveByIdAndProfessorId(deleted.getId(), "super")).isFalse();
        assertThat(repository.existsActiveByIdAndProfessorId(Long.MAX_VALUE, "super")).isFalse();
        assertThat(repository.lockActiveByIdAndProfessorId(Long.MAX_VALUE, "super")).isFalse();
        superAdmin.setDeletedAt(LocalDateTime.now());
        entityManager.flush();
        assertThat(repository.findAllByProfessorId("super")).isEmpty();
        assertThat(repository.existsActiveByIdAndProfessorId(active.getId(), "super")).isFalse();
        assertThat(repository.lockActiveByIdAndProfessorId(active.getId(), "super")).isFalse();
    }

    @Test
    void superAdminSeesAllActiveCoursesWhileAdminKeepsExistingScope() {
        UserJpaEntity professor = user("professor", UserGlobalRole.ADMIN);
        user("super", UserGlobalRole.SUPER_ADMIN);
        user("other", UserGlobalRole.ADMIN);
        CourseJpaEntity occupied = course("다른 교수 강좌");
        CourseJpaEntity empty = course("빈 강좌");
        CourseJpaEntity deleted = course("삭제 강좌");
        deleted.setDeletedAt(LocalDateTime.now());
        section(professor, occupied, "01", false);
        entityManager.flush();
        assertThat(courses.findAllOwnedByProfessor("super"))
            .extracting(CourseJpaEntity::getId).containsExactlyInAnyOrder(occupied.getId(), empty.getId());
        assertThat(courses.findAllOwnedByProfessor("other"))
            .extracting(CourseJpaEntity::getId).containsExactly(empty.getId());
    }

    @ParameterizedTest
    @CsvSource({"super,false", "super,true", "professor,false", "professor,true"})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void sectionLockBlocksAnotherConnectionUntilCommitOrRollback(String caller, boolean rollback) throws SQLException {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long sectionId = tx.execute(status -> {
            UserJpaEntity professor = user("professor", UserGlobalRole.ADMIN);
            user("super", UserGlobalRole.SUPER_ADMIN);
            return section(professor, course("잠금 검증"), "01", false).getId();
        });

        try (Connection contender = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            contender.setAutoCommit(false);
            tx.executeWithoutResult(status -> {
                assertThat(repository.lockActiveByIdAndProfessorId(sectionId, caller)).isTrue();
                // 별도 PostgreSQL 세션이 실제로 같은 행의 잠금을 기다리는지 확인한다.
                assertThatThrownBy(() -> lockFromConnection(contender, sectionId))
                    .isInstanceOfSatisfying(SQLException.class,
                        exception -> assertThat(exception.getSQLState()).isEqualTo("55P03"));
                if (rollback) status.setRollbackOnly();
            });
            contender.rollback();
            lockFromConnection(contender, sectionId);
            contender.rollback();
        } finally {
            tx.executeWithoutResult(status -> {
                entityManager.createQuery("delete from SectionJpaEntity").executeUpdate();
                entityManager.createQuery("delete from CourseJpaEntity").executeUpdate();
                entityManager.createQuery("delete from UserJpaEntity").executeUpdate();
            });
        }
    }

    private void lockFromConnection(Connection connection, Long sectionId) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("set local lock_timeout = '300ms'");
        }
        try (var statement = connection.prepareStatement("select id from section where id = ? for update")) {
            statement.setLong(1, sectionId);
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(sectionId);
            }
        }
    }

    private UserJpaEntity user(String id, UserGlobalRole role) {
        UserJpaEntity user = UserJpaEntity.builder().studentNumber(id).email(id + "@kgu.ac.kr")
            .name(id).password("encoded").phone("01000000000").globalRole(role).build();
        entityManager.persist(user);
        return user;
    }

    private CourseJpaEntity course(String name) {
        CourseJpaEntity course = CourseJpaEntity.builder().name(name).year(2026)
            .semester(SemesterType.SPRING).status(StatusType.ACTIVE).build();
        entityManager.persist(course);
        return course;
    }

    private SectionJpaEntity section(UserJpaEntity professor, CourseJpaEntity course, String code, boolean deleted) {
        SectionJpaEntity section = SectionJpaEntity.builder().professor(professor).course(course)
            .code(code).name(code).classTime("월123").capacity(40).build();
        if (deleted) section.setDeletedAt(LocalDateTime.now());
        entityManager.persist(section);
        return section;
    }
}
