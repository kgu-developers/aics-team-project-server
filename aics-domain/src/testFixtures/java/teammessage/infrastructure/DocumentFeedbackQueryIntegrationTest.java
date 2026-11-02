package teammessage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.EntityManager;
import kgu.developers.domain.teammessage.domain.DocumentFeedbackSummary;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.infrastructure.TeamMessageJpaEntity;
import kgu.developers.domain.teammessage.infrastructure.TeamMessageRepositoryImpl;
import kgu.developers.domain.teamthread.infrastructure.TeamThreadJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:document-feedback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TeamMessageRepositoryImpl.class)
class DocumentFeedbackQueryIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 15, 0);

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = {TeamMessageJpaEntity.class, TeamThreadJpaEntity.class})
    @EnableJpaRepositories("kgu.developers.domain.teammessage.infrastructure")
    static class Config { }

    @Autowired EntityManager entityManager;
    @Autowired TeamMessageRepositoryImpl repository;

    @Test
    void countsProfessorFeedbackAndSeparatesDocumentTypeIdAndTeam() {
        var first = thread(1L);
        var second = thread(2L);
        message(first, "professor", TeamMessageRelatedType.PROPOSAL, 10L, NOW);
        message(first, "professor", TeamMessageRelatedType.PROPOSAL, 10L, NOW.plusMinutes(1));
        message(first, "student", TeamMessageRelatedType.PROPOSAL, 10L, NOW.plusMinutes(2));
        message(first, "professor", TeamMessageRelatedType.MID_REPORT, 10L, NOW.plusMinutes(3));
        message(first, "professor", TeamMessageRelatedType.PROPOSAL, 11L, NOW.plusMinutes(4));
        message(first, "professor", TeamMessageRelatedType.GENERAL, null, NOW.plusMinutes(5));
        message(second, "professor", TeamMessageRelatedType.PROPOSAL, 10L, NOW.plusMinutes(6));
        var deleted = message(first, "professor", TeamMessageRelatedType.PROPOSAL, 10L, NOW.plusMinutes(7));
        deleted.delete();
        entityManager.flush();

        var result = repository.summarizeFeedback(List.of(1L), TeamMessageRelatedType.PROPOSAL,
                List.of(10L, 12L), "professor");

        assertThat(result).containsOnlyKeys(10L);
        assertThat(result.get(10L)).isEqualTo(new DocumentFeedbackSummary(2L, NOW.plusMinutes(1), NOW.plusMinutes(2)));
    }

    @Test
    void summarizesMultipleDocumentsIncludingStudentOnlyConversation() {
        var first = thread(1L);
        var second = thread(2L);
        message(first, "student", TeamMessageRelatedType.MID_REPORT, 10L, NOW);
        message(second, "professor", TeamMessageRelatedType.MID_REPORT, 20L, NOW);

        var result = repository.summarizeFeedback(List.of(1L, 2L), TeamMessageRelatedType.MID_REPORT,
                List.of(10L, 20L), "professor");

        assertThat(result.get(10L)).isEqualTo(new DocumentFeedbackSummary(0L, null, NOW));
        assertThat(result.get(20L)).isEqualTo(new DocumentFeedbackSummary(1L, NOW, null));
        assertThat(repository.summarizeFeedback(List.of(), TeamMessageRelatedType.MID_REPORT, List.of(10L), "professor")).isEmpty();
        assertThat(repository.summarizeFeedback(List.of(1L), TeamMessageRelatedType.MID_REPORT, List.of(), "professor")).isEmpty();
    }

    private TeamThreadJpaEntity thread(Long teamId) {
        var thread = TeamThreadJpaEntity.builder().teamId(teamId).build();
        entityManager.persist(thread);
        return thread;
    }

    private TeamMessageJpaEntity message(TeamThreadJpaEntity thread, String sender,
            TeamMessageRelatedType type, Long relatedId, LocalDateTime at) {
        var message = TeamMessageJpaEntity.builder().threadId(thread.getId()).senderId(sender)
                .message("메시지").relatedType(type).relatedId(relatedId).build();
        entityManager.persist(message);
        entityManager.flush();
        entityManager.createNativeQuery("update \"team_message\" set created_at = :at where id = :id")
                .setParameter("at", at).setParameter("id", message.getId()).executeUpdate();
        return message;
    }
}
