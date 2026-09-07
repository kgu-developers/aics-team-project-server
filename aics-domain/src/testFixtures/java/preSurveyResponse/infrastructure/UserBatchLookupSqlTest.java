package preSurveyResponse.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManagerFactory;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.infrastructure.JpaUserRepository;
import kgu.developers.domain.user.infrastructure.UserRepositoryImpl;

/**
 * 어드민 사전조사 목록이 응답자·희망 조원 이름을 채울 때 학번마다 쿼리를 날리지 않고 IN 절 한 번으로
 * 끝내는지, 실제로 나간 SQL 개수를 세어 확인한다(N+1 회귀 방지).
 */
@SpringBootTest(
    classes = UserBatchLookupSqlTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:user_batch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true"
    })
class UserBatchLookupSqlTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("학번 목록 조회는 학번 개수와 무관하게 쿼리 한 번으로 끝난다")
    void findAllByStudentNumberInIssuesSingleQuery() {
        List<String> studentNumbers = List.of("A1", "A2", "A3", "A4", "A5");
        studentNumbers.forEach(number -> userRepository.save(
            User.create(number, number + "@kyonggi.ac.kr", "이름" + number, "pw", UserGlobalRole.USER, "010-0000-0000")));

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<User> found = userRepository.findAllByStudentNumberIn(studentNumbers);

        assertThat(found).hasSize(5);
        assertThat(statistics.getPrepareStatementCount())
            .as("학번 5개를 채우는 데 나간 쿼리 수")
            .isEqualTo(1);
    }

    @Configuration
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class
    })
    @EntityScan("kgu.developers")
    @EnableJpaRepositories(basePackageClasses = JpaUserRepository.class)
    @Import(UserRepositoryImpl.class)
    static class TestConfig {
    }
}
