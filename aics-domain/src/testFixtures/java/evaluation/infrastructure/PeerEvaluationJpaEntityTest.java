package evaluation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import kgu.developers.domain.evaluation.domain.Grade;
import kgu.developers.domain.evaluation.domain.PeerEvaluationAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestion;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestionType;
import kgu.developers.domain.evaluation.domain.PeerEvaluationResponse;
import kgu.developers.domain.evaluation.infrastructure.GradeJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationAnswerJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationFormJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationQuestionJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationResponseJpaEntity;

class PeerEvaluationJpaEntityTest {
    private final LocalDateTime opensAt = LocalDateTime.of(2026, 8, 1, 9, 0);
    private final LocalDateTime closesAt = LocalDateTime.of(2026, 8, 7, 18, 0);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 9, 0);
    private final LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 2, 9, 0);
    private final LocalDateTime deletedAt = LocalDateTime.of(2026, 7, 3, 9, 0);

    @Test
    @DisplayName("상호평가 양식 JPA 엔티티는 도메인과 왕복 매핑된다")
    void formRoundTrip() {
        PeerEvaluationForm domain = PeerEvaluationForm.restore(1L, 2L, 3L, true, opensAt, closesAt, createdAt, updatedAt, deletedAt);

        PeerEvaluationForm mapped = PeerEvaluationFormJpaEntity.toEntity(domain).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getSectionId()).isEqualTo(2L);
        assertThat(mapped.getMilestoneId()).isEqualTo(3L);
        assertThat(mapped.isAnonymous()).isTrue();
        assertThat(mapped.getOpensAt()).isEqualTo(opensAt);
        assertThat(mapped.getClosesAt()).isEqualTo(closesAt);
        assertThat(mapped.getCreatedAt()).isEqualTo(createdAt);
        assertThat(mapped.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("상호평가 질문 JPA 엔티티는 질문 유형과 최대 점수를 보존한다")
    void questionRoundTrip() {
        PeerEvaluationQuestion domain = PeerEvaluationQuestion.restore(1L, 2L, "협업 태도를 평가하세요.", PeerEvaluationQuestionType.SCALE, new BigDecimal("5.00"), 1, createdAt, updatedAt, deletedAt);

        PeerEvaluationQuestion mapped = PeerEvaluationQuestionJpaEntity.toEntity(domain).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getFormId()).isEqualTo(2L);
        assertThat(mapped.getType()).isEqualTo(PeerEvaluationQuestionType.SCALE);
        assertThat(mapped.getMaxScore()).isEqualByComparingTo("5.00");
        assertThat(mapped.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("상호평가 응답 JPA 엔티티는 제출 시각과 본인 기여도를 보존한다")
    void responseRoundTrip() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        PeerEvaluationResponse domain = PeerEvaluationResponse.restore(1L, 2L, "20260001", "20260002", submittedAt, new BigDecimal("40.00"), "좋았습니다.", createdAt, updatedAt, deletedAt);

        PeerEvaluationResponse mapped = PeerEvaluationResponseJpaEntity.toEntity(domain).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getFormId()).isEqualTo(2L);
        assertThat(mapped.getEvaluatorId()).isEqualTo("20260001");
        assertThat(mapped.getTargetId()).isEqualTo("20260002");
        assertThat(mapped.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(mapped.getSelfContribution()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("상호평가 답변 JPA 엔티티는 점수형 답변을 보존한다")
    void answerRoundTrip() {
        PeerEvaluationAnswer domain = PeerEvaluationAnswer.restore(1L, 2L, 3L, new BigDecimal("4.50"), null, createdAt, updatedAt, deletedAt);

        PeerEvaluationAnswer mapped = PeerEvaluationAnswerJpaEntity.toEntity(domain).toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getResponseId()).isEqualTo(2L);
        assertThat(mapped.getQuestionId()).isEqualTo(3L);
        assertThat(mapped.getScore()).isEqualByComparingTo("4.50");
        assertThat(mapped.getTextAnswer()).isNull();
    }

    @Test
    @DisplayName("성적 JPA 엔티티는 JSON 스냅샷 문자열을 그대로 보존한다")
    void gradeRoundTrip() {
        String snapshot = "{\"finalScore\":94.5}";
        LocalDateTime finalizedAt = LocalDateTime.of(2026, 8, 14, 10, 0);
        Grade domain = Grade.restore(1L, 2L, 3L, "20260001", new BigDecimal("90.00"), new BigDecimal("1.0500"), new BigDecimal("94.50"), BigDecimal.ZERO, "점수 확정", snapshot, finalizedAt, createdAt, updatedAt, deletedAt);

        GradeJpaEntity entity = GradeJpaEntity.toEntity(domain);
        Grade mapped = entity.toDomain();

        assertThat(mapped.getId()).isEqualTo(1L);
        assertThat(mapped.getSectionId()).isEqualTo(2L);
        assertThat(mapped.getTeamId()).isEqualTo(3L);
        assertThat(mapped.getUserId()).isEqualTo("20260001");
        assertThat(entity.getSnapshot().isObject()).isTrue();
        assertThat(entity.getSnapshot().get("finalScore").decimalValue()).isEqualByComparingTo("94.5");
        assertThat(mapped.getAdjustmentReason()).isEqualTo("점수 확정");
        assertThat(mapped.getSnapshot()).isEqualTo(snapshot);
        assertThat(mapped.getFinalizedAt()).isEqualTo(finalizedAt);
    }

    @Test
    @DisplayName("성적 JPA 엔티티는 확정 전 nullable 값을 보존한다")
    void gradeDraftRoundTrip() {
        Grade domain = Grade.restore(1L, 2L, 3L, "20260001", null, null, null, null, null, null, null, createdAt, updatedAt, deletedAt);

        Grade mapped = GradeJpaEntity.toEntity(domain).toDomain();

        assertThat(mapped.getTeamScore()).isNull();
        assertThat(mapped.getSnapshot()).isNull();
        assertThat(mapped.getFinalizedAt()).isNull();
    }

    @Test
    @DisplayName("성적 JPA 엔티티는 grade 테이블과 JSONB 문자열 매핑을 사용한다")
    void gradeJpaMetadata() throws NoSuchFieldException {
        Table table = GradeJpaEntity.class.getAnnotation(Table.class);
        Field userId = GradeJpaEntity.class.getDeclaredField("userId");
        Field snapshot = GradeJpaEntity.class.getDeclaredField("snapshot");

        assertThat(table.name()).isEqualTo("grade");
        assertThat(userId.getAnnotation(Column.class).length()).isEqualTo(20);
        assertThat(snapshot.getAnnotation(Column.class).nullable()).isTrue();
        assertThat(snapshot.getAnnotation(Column.class).columnDefinition()).isEqualTo("jsonb");
        assertThat(snapshot.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
        assertThat(snapshot.getType()).isEqualTo(JsonNode.class);
    }

    @Test
    @DisplayName("상호평가 응답 JPA 엔티티는 사용자 엔티티와 같은 학번 길이를 사용한다")
    void responseJpaMetadata() throws NoSuchFieldException {
        Field evaluatorId = PeerEvaluationResponseJpaEntity.class.getDeclaredField("evaluatorId");
        Field targetId = PeerEvaluationResponseJpaEntity.class.getDeclaredField("targetId");

        assertThat(evaluatorId.getAnnotation(Column.class).length()).isEqualTo(20);
        assertThat(targetId.getAnnotation(Column.class).length()).isEqualTo(20);
    }
}
