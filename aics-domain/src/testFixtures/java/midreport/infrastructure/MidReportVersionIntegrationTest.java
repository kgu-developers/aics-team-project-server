package midreport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.time.LocalDateTime;
import java.util.Map;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.exception.MidReportVersionConflictException;
import kgu.developers.domain.midreport.infrastructure.MidReportRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mid-report-version;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(MidReportRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = NOT_SUPPORTED)
class MidReportVersionIntegrationTest {
    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.midreport.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.midreport.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private MidReportRepositoryImpl repository;

    private TransactionTemplate tx;

    @Autowired
    void setTransactionTemplate(PlatformTransactionManager transactionManager) {
        tx = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("영역 저장은 부모 문서 버전을 증가시키고 오래된 요청을 거부한다")
    void incrementsVersionForBlockUpdate() {
        MidReport first = tx.execute(status -> repository.save(report()));
        assertThat(first.getVersion()).isZero();

        MidReport stale = tx.execute(status -> repository.findById(first.getId()).orElseThrow());
        MidReport updated = tx.execute(status -> {
            MidReport current = repository.findById(first.getId()).orElseThrow();
            current.updateBlock("topic", 0L, topicFields("먼저 저장"), "202600001", LocalDateTime.now());
            return repository.save(current);
        });

        assertThat(updated.getVersion()).isEqualTo(1L);
        stale.updateBlock("topic", 0L, topicFields("나중 저장"), "202600002", LocalDateTime.now());
        assertThatThrownBy(() -> tx.execute(status -> repository.save(stale)))
            .isInstanceOf(MidReportVersionConflictException.class);
    }

    private MidReport report() {
        return MidReport.create(
            10L,
            20L,
            "CineFlow 중간보고서",
            LocalDateTime.of(2026, 10, 26, 23, 59),
            "CineFlow",
            "영화관 관리"
        );
    }

    private com.fasterxml.jackson.databind.JsonNode topicFields(String title) {
        return MidReportBlockDefinition.TOPIC.emptyFields(Map.of(
            "title", title,
            "description", "설명"
        ));
    }
}
