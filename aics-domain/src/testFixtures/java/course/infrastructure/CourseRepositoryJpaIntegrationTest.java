package course.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.infrastructure.CourseJpaEntity;
import kgu.developers.domain.course.infrastructure.CourseRepositoryImpl;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

/**
 * findAllOwnedByProfessor는 "본인 분반이 있는 강좌" OR "분반이 아예 없는 강좌"를 반환해야 한다.
 * (PR #309 리뷰에서 지적됨: 분반이 0개인 강좌까지 걸러내면 강좌를 막 만들거나 아직 아무도 분반을
 * 안 붙인 강좌가 모든 교수에게서 사라져 첫 분반을 붙일 courseId를 고를 수 없게 된다.)
 * OR/NOT IN이 섞인 JPQL이라 Mockito 단위 테스트로는 실제 쿼리 동작을 검증할 수 없어 실제
 * PostgreSQL(Testcontainers)로 검증한다.
 */
@DataJpaTest
@Testcontainers
@Import(CourseRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(propagation = NOT_SUPPORTED)
class CourseRepositoryJpaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @SpringBootConfiguration
    @EntityScan("kgu.developers")
    @EnableJpaRepositories("kgu.developers")
    static class TestConfig {
    }

    @Autowired
    private CourseRepositoryImpl repository;

    @Autowired
    private EntityManager entityManager;

    private TransactionTemplate tx;

    private static final String PROFESSOR_ID = "202000001";
    private static final String OTHER_PROFESSOR_ID = "202000002";

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

            persistProfessor(PROFESSOR_ID, "본인교수");
            persistProfessor(OTHER_PROFESSOR_ID, "타교수");
        });
    }

    @Test
    @DisplayName("본인 분반이 있는 강좌, 분반이 아예 없는 강좌는 포함하고, 타 교수 분반만 있는 강좌는 제외한다")
    void findsOwnedAndSectionlessCoursesOnly() {
        Long ownedCourseId = tx.execute(status -> {
            CourseJpaEntity course = persistCourse("본인분반 있는 강좌");
            persistSection(course, PROFESSOR_ID, "1154");
            return course.getId();
        });
        Long sectionlessCourseId = tx.execute(status -> persistCourse("분반 없는 강좌").getId());
        Long otherOnlyCourseId = tx.execute(status -> {
            CourseJpaEntity course = persistCourse("타 교수 분반만 있는 강좌");
            persistSection(course, OTHER_PROFESSOR_ID, "1155");
            return course.getId();
        });

        List<Course> result = tx.execute(status -> repository.findAllOwnedByProfessor(PROFESSOR_ID));

        assertThat(result).extracting(Course::getId)
                .containsExactlyInAnyOrder(ownedCourseId, sectionlessCourseId)
                .doesNotContain(otherOnlyCourseId);
    }

    private void persistProfessor(String studentNumber, String name) {
        entityManager.persist(UserJpaEntity.builder()
                .studentNumber(studentNumber)
                .email(studentNumber + "@kgu.ac.kr")
                .name(name)
                .password("password")
                .globalRole(UserGlobalRole.ADMIN)
                .phone("01000000000")
                .build());
    }

    private CourseJpaEntity persistCourse(String name) {
        CourseJpaEntity course = CourseJpaEntity.builder()
                .name(name)
                .year(2026)
                .semester(SemesterType.SPRING)
                .status(StatusType.ACTIVE)
                .build();
        entityManager.persist(course);
        entityManager.flush();
        return course;
    }

    private void persistSection(CourseJpaEntity course, String professorId, String code) {
        entityManager.persist(SectionJpaEntity.builder()
                .professor(entityManager.getReference(UserJpaEntity.class, professorId))
                .course(course)
                .code(code)
                .name(code)
                .classTime("월1,2")
                .capacity(40)
                .build());
        entityManager.flush();
    }
}
