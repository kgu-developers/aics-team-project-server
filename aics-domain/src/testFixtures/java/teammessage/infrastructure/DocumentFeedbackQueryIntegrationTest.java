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
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    @EntityScan(basePackageClasses = {TeamMessageJpaEntity.class, TeamThreadJpaEntity.class, UserJpaEntity.class})
    @EnableJpaRepositories("kgu.developers.domain.teammessage.infrastructure")
    static class Config { }

    @Autowired EntityManager entityManager;
    @Autowired TeamMessageRepositoryImpl repository;

    @BeforeEach
    void users() {
        user("professor", UserGlobalRole.ADMIN);
        user("student", UserGlobalRole.USER);
    }

    @ParameterizedTest
    @EnumSource(value = TeamMessageRelatedType.class, names = {"PROPOSAL", "MID_REPORT"})
    void professorChangePreservesFeedbackAndStudentReplyClassification(TeamMessageRelatedType type) {
        var previousProfessor = user("professor-before", UserGlobalRole.ADMIN);
        user("professor-after", UserGlobalRole.ADMIN);
        var thread = thread(1L);
        message(thread, "professor-before", type, 10L, NOW);
        previousProfessor.delete();
        entityManager.flush();

        var feedback = repository.summarizeFeedback(List.of(1L), type, List.of(10L)).get(10L);
        assertThat(feedback).isEqualTo(new DocumentFeedbackSummary(1L, NOW, null));
        assertThat(feedback.feedbackRequested(null)).isFalse();

        message(thread, "student", type, 10L, NOW.plusMinutes(1));
        var awaiting = repository.summarizeFeedback(List.of(1L), type, List.of(10L)).get(10L);
        assertThat(awaiting.feedbackCount()).isEqualTo(1L);
        assertThat(awaiting.feedbackRequested(null)).isTrue();

        message(thread, "professor-after", type, 10L, NOW.plusMinutes(2));
        var answered = repository.summarizeFeedback(List.of(1L), type, List.of(10L)).get(10L);
        assertThat(answered).isEqualTo(new DocumentFeedbackSummary(2L, NOW.plusMinutes(2), NOW.plusMinutes(1)));
        assertThat(answered.feedbackRequested(null)).isFalse();
    }

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
                List.of(10L, 12L));

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
                List.of(10L, 20L));

        assertThat(result.get(10L)).isEqualTo(new DocumentFeedbackSummary(0L, null, NOW));
        assertThat(result.get(20L)).isEqualTo(new DocumentFeedbackSummary(1L, NOW, null));
        assertThat(repository.summarizeFeedback(List.of(), TeamMessageRelatedType.MID_REPORT, List.of(10L))).isEmpty();
        assertThat(repository.summarizeFeedback(List.of(1L), TeamMessageRelatedType.MID_REPORT, List.of())).isEmpty();
    }

    private TeamThreadJpaEntity thread(Long teamId) {
        var thread = TeamThreadJpaEntity.builder().teamId(teamId).build();
        entityManager.persist(thread);
        return thread;
    }

    private UserJpaEntity user(String id, UserGlobalRole role) {
        var user = UserJpaEntity.builder().studentNumber(id).email(id + "@kgu.ac.kr")
                .name(id).password("password").globalRole(role).phone("01000000000").build();
        entityManager.persist(user);
        return user;
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
