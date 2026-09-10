package evaluation.infrastructure;

import java.util.List;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationTeammateAnswerRepositoryImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// (submission_id, target_user_id) 유니크 제약이 실제 DB 플러시 순서에서도 지켜지는지 확인한다.
// @GeneratedValue(IDENTITY)는 saveAll()의 INSERT를 즉시 실행하는데, deleteAllBySubmissionId()가
// 파생 삭제 메서드(엔티티 단위 지연 삭제)였을 때는 그 DELETE가 flush 시점까지 밀려서, 재저장 시
// 아직 지워지지 않은 이전 행과 유니크 제약이 충돌했다(KD3-227). Mock 기반 단위 테스트로는 이
// flush 순서 문제를 재현할 수 없어 실제 H2(PostgreSQL 모드) + 진짜 리포지토리로 검증한다.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:peer-eval-resave;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(PeerEvaluationTeammateAnswerRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PeerEvaluationTeammateAnswerResaveIntegrationTest {
    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.evaluation.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.evaluation.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private PeerEvaluationTeammateAnswerRepositoryImpl repository;

    @Test
    @DisplayName("같은 submissionId로 여러 번 재저장(delete-then-insert)해도 유니크 제약과 충돌하지 않는다")
    void resavingSameSubmissionDoesNotViolateUniqueConstraint() {
        Long submissionId = 1L;

        repository.deleteAllBySubmissionId(submissionId);
        List<PeerEvaluationTeammateAnswer> first = repository.saveAll(List.of(
            answer(submissionId, "202611111"),
            answer(submissionId, "202611112")
        ));
        Assertions.assertThat(first).hasSize(2);

        // 두 번째 저장(임시저장 재시도에 해당) — 같은 대상 학번으로 그대로 재저장
        repository.deleteAllBySubmissionId(submissionId);
        List<PeerEvaluationTeammateAnswer> second = repository.saveAll(List.of(
            answer(submissionId, "202611111"),
            answer(submissionId, "202611112")
        ));
        Assertions.assertThat(second).hasSize(2);

        // 세 번째 저장(최종 제출에 해당) — 대상 학번 구성이 줄어드는 경우까지 확인
        repository.deleteAllBySubmissionId(submissionId);
        List<PeerEvaluationTeammateAnswer> third = repository.saveAll(List.of(
            answer(submissionId, "202611111")
        ));
        Assertions.assertThat(third).hasSize(1);
        Assertions.assertThat(repository.findAllBySubmissionId(submissionId)).hasSize(1);
    }

    private PeerEvaluationTeammateAnswer answer(Long submissionId, String targetUserId) {
        return PeerEvaluationTeammateAnswer.create(submissionId, targetUserId, 50, "상세", "평가");
    }
}
