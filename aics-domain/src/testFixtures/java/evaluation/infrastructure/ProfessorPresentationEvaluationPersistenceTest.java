package evaluation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluation;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScore;
import kgu.developers.domain.evaluation.infrastructure.ProfessorPresentationEvaluationRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.ProfessorPresentationEvaluationScoreRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:professor-presentation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProfessorPresentationEvaluationRepositoryImpl.class, ProfessorPresentationEvaluationScoreRepositoryImpl.class})
class ProfessorPresentationEvaluationPersistenceTest {
    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.evaluation.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.evaluation.infrastructure")
    static class TestConfig {
    }

    @Autowired private ProfessorPresentationEvaluationRepositoryImpl evaluations;
    @Autowired private ProfessorPresentationEvaluationScoreRepositoryImpl scores;

    @Test
    void resaveKeepsEvaluationAndReplacesScoresWithoutDuplicateRows() {
        ProfessorPresentationEvaluation evaluation = ProfessorPresentationEvaluation.create(3L, 20L);
        evaluation.submit("professor", "첫 메모", LocalDateTime.of(2026, 10, 2, 10, 0));
        Long id = evaluations.save(evaluation).getId();
        scores.replaceAll(id, List.of(ProfessorPresentationEvaluationScore.create(id, 1L, 8, 10)));

        ProfessorPresentationEvaluation existing = evaluations.findByMilestoneIdAndTeamId(3L, 20L).orElseThrow();
        existing.submit("professor", "수정 메모", LocalDateTime.of(2026, 10, 2, 11, 0));
        assertThat(evaluations.save(existing).getId()).isEqualTo(id);
        scores.replaceAll(id, List.of(ProfessorPresentationEvaluationScore.create(id, 1L, 9, 10)));

        assertThat(evaluations.findByMilestoneIdAndTeamId(3L, 20L).orElseThrow().getMemo()).isEqualTo("수정 메모");
        assertThat(scores.findAllByEvaluationId(id)).singleElement()
                .satisfies(score -> assertThat(score.score()).isEqualTo(9));
        assertThat(scores.existsByCriterionId(1L)).isTrue();
    }
}
