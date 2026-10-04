package midreport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.domain.MidReportStatus;
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

    @Test
    @DisplayName("학생 저장이 먼저 반영되면 오래된 교수 피드백 완료 저장을 거부한다")
    void rejectsStaleRevisionCompletionAfterStudentEdit() {
        MidReport first = tx.execute(status -> repository.save(revisionRequestedReport()));
        MidReport staleCompletion = tx.execute(status -> repository.findById(first.getId()).orElseThrow());

        MidReport studentEdited = tx.execute(status -> {
            MidReport current = repository.findById(first.getId()).orElseThrow();
            current.updateBlock(
                "topic", current.getVersion(), topicFields("학생이 먼저 저장"), "202600001",
                LocalDateTime.of(2026, 9, 9, 10, 30)
            );
            return repository.save(current);
        });
        assertThat(studentEdited.getVersion()).isEqualTo(1L);

        staleCompletion.completeRevision(
            staleCompletion.getVersion(), "professor-1", LocalDateTime.of(2026, 9, 9, 11, 0)
        );

        assertThatThrownBy(() -> tx.execute(status -> repository.save(staleCompletion)))
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

    private MidReport revisionRequestedReport() {
        MidReport created = MidReport.create(
            11L,
            21L,
            "CineFlow 중간보고서",
            LocalDateTime.of(2026, 10, 26, 23, 59),
            "CineFlow",
            "영화관 관리"
        );
        MidReport submitted = MidReport.builder()
            .teamId(created.getTeamId())
            .milestoneId(created.getMilestoneId())
            .title(created.getTitle())
            .dueDate(created.getDueDate())
            .status(MidReportStatus.SUBMITTED)
            .submittedAt(LocalDateTime.of(2026, 9, 8, 14, 0))
            .submittedBy("202600001")
            .blocks(created.getBlocks())
            .build();
        submitted.requestRevision(List.of("topic"), LocalDateTime.of(2026, 9, 9, 10, 0));
        return submitted;
    }

    private com.fasterxml.jackson.databind.JsonNode topicFields(String title) {
        return MidReportBlockDefinition.TOPIC.emptyFields(Map.of(
            "title", title,
            "description", "설명"
        ));
    }
}
