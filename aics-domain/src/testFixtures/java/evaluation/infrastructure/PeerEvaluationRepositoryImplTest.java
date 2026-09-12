package evaluation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.evaluation.domain.Grade;
import kgu.developers.domain.evaluation.domain.PeerEvaluationAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestion;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestionType;
import kgu.developers.domain.evaluation.domain.PeerEvaluationResponse;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.infrastructure.GradeJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.GradeRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.JpaGradeRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaPeerEvaluationAnswerRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaPeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaPeerEvaluationQuestionRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaPeerEvaluationResponseRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaPeerEvaluationSubmissionRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaPeerEvaluationTeammateAnswerRepository;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationAnswerJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationAnswerRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationFormJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationFormRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationQuestionJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationQuestionRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationResponseJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationResponseRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationSubmissionJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationSubmissionRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationTeammateAnswerJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.PeerEvaluationTeammateAnswerRepositoryImpl;

@ExtendWith(MockitoExtension.class)
class PeerEvaluationRepositoryImplTest {
    private final LocalDateTime opensAt = LocalDateTime.of(2026, 8, 1, 9, 0);
    private final LocalDateTime closesAt = LocalDateTime.of(2026, 8, 7, 18, 0);

    @Mock
    private JpaPeerEvaluationFormRepository formJpaRepository;

    @Mock
    private JpaPeerEvaluationQuestionRepository questionJpaRepository;

    @Mock
    private JpaPeerEvaluationResponseRepository responseJpaRepository;

    @Mock
    private JpaPeerEvaluationAnswerRepository answerJpaRepository;

    @Mock
    private JpaGradeRepository gradeJpaRepository;

    @Mock
    private JpaPeerEvaluationTeammateAnswerRepository teammateAnswerJpaRepository;

    @Mock
    private JpaPeerEvaluationSubmissionRepository submissionJpaRepository;

    @Test
    @DisplayName("상호평가 양식 저장소 어댑터는 저장 후 도메인을 반환한다")
    void saveForm() {
        PeerEvaluationForm form = PeerEvaluationForm.create(1L, 2L, true, opensAt, closesAt);
        PeerEvaluationForm saved = PeerEvaluationForm.restore(10L, 1L, 2L, true, opensAt, closesAt, null, null, null);
        given(formJpaRepository.save(any(PeerEvaluationFormJpaEntity.class))).willReturn(PeerEvaluationFormJpaEntity.toEntity(saved));
        PeerEvaluationFormRepositoryImpl repository = new PeerEvaluationFormRepositoryImpl(formJpaRepository);

        PeerEvaluationForm result = repository.save(form);

        assertThat(result.getId()).isEqualTo(10L);
        ArgumentCaptor<PeerEvaluationFormJpaEntity> captor = ArgumentCaptor.forClass(PeerEvaluationFormJpaEntity.class);
        verify(formJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getSectionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("상호평가 양식 저장소 어댑터는 삭제되지 않은 양식만 조회한다")
    void findFormById() {
        PeerEvaluationForm saved = PeerEvaluationForm.restore(10L, 1L, 2L, true, opensAt, closesAt, null, null, null);
        given(formJpaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(PeerEvaluationFormJpaEntity.toEntity(saved)));
        PeerEvaluationFormRepositoryImpl repository = new PeerEvaluationFormRepositoryImpl(formJpaRepository);

        Optional<PeerEvaluationForm> result = repository.findById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("상호평가 양식 저장소 어댑터는 분반의 최신 양식부터 조회한다")
    void findFormsBySectionId() {
        PeerEvaluationForm latest = PeerEvaluationForm.restore(11L, 1L, 3L, true, opensAt, closesAt, null, null, null);
        PeerEvaluationForm previous = PeerEvaluationForm.restore(10L, 1L, 2L, true, opensAt, closesAt, null, null, null);
        given(formJpaRepository.findAllBySectionIdAndDeletedAtIsNullOrderByIdDesc(1L)).willReturn(List.of(
            PeerEvaluationFormJpaEntity.toEntity(latest),
            PeerEvaluationFormJpaEntity.toEntity(previous)
        ));
        PeerEvaluationFormRepositoryImpl repository = new PeerEvaluationFormRepositoryImpl(formJpaRepository);

        List<PeerEvaluationForm> result = repository.findAllBySectionIdOrderByIdDesc(1L);

        assertThat(result).extracting(PeerEvaluationForm::getId).containsExactly(11L, 10L);
    }

    @Test
    @DisplayName("상호평가 질문 저장소 어댑터는 표시 순서로 질문 목록을 조회한다")
    void findQuestionsByFormId() {
        PeerEvaluationQuestion first = PeerEvaluationQuestion.restore(1L, 10L, "협업 태도", PeerEvaluationQuestionType.SCALE, new BigDecimal("5.00"), 0, null, null, null);
        PeerEvaluationQuestion second = PeerEvaluationQuestion.restore(2L, 10L, "개선 의견", PeerEvaluationQuestionType.TEXT, null, 1, null, null, null);
        given(questionJpaRepository.findAllByFormIdAndDeletedAtIsNullOrderByDisplayOrderAsc(10L))
                .willReturn(List.of(PeerEvaluationQuestionJpaEntity.toEntity(first), PeerEvaluationQuestionJpaEntity.toEntity(second)));
        PeerEvaluationQuestionRepositoryImpl repository = new PeerEvaluationQuestionRepositoryImpl(questionJpaRepository);

        List<PeerEvaluationQuestion> result = repository.findAllByFormIdOrderByDisplayOrder(10L);

        assertThat(result).extracting(PeerEvaluationQuestion::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("상호평가 응답 저장소 어댑터는 평가자와 대상자 조합으로 응답을 조회한다")
    void findResponseByPair() {
        PeerEvaluationResponse response = PeerEvaluationResponse.restore(1L, 10L, "20260001", "20260002", null, BigDecimal.TEN, "좋았습니다.", null, null, null);
        given(responseJpaRepository.findByFormIdAndEvaluatorIdAndTargetIdAndDeletedAtIsNull(10L, "20260001", "20260002"))
                .willReturn(Optional.of(PeerEvaluationResponseJpaEntity.toEntity(response)));
        PeerEvaluationResponseRepositoryImpl repository = new PeerEvaluationResponseRepositoryImpl(responseJpaRepository);

        Optional<PeerEvaluationResponse> result = repository.findByFormIdAndEvaluatorIdAndTargetId(
                10L,
                " 20260001 ",
                " 20260002 "
        );

        assertThat(result).isPresent();
        assertThat(result.get().getEvaluatorId()).isEqualTo("20260001");
        assertThat(result.get().getTargetId()).isEqualTo("20260002");
    }

    @Test
    @DisplayName("상호평가 답변 저장소 어댑터는 응답 식별자로 답변 목록을 조회한다")
    void findAnswersByResponseId() {
        PeerEvaluationAnswer answer = PeerEvaluationAnswer.restore(1L, 10L, 20L, new BigDecimal("4.00"), null, null, null, null);
        given(answerJpaRepository.findAllByResponseIdAndDeletedAtIsNull(10L))
                .willReturn(List.of(PeerEvaluationAnswerJpaEntity.toEntity(answer)));
        PeerEvaluationAnswerRepositoryImpl repository = new PeerEvaluationAnswerRepositoryImpl(answerJpaRepository);

        List<PeerEvaluationAnswer> result = repository.findAllByResponseId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestionId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("성적 저장소 어댑터는 분반과 팀 식별자로 성적 목록을 조회한다")
    void findGradesBySectionAndTeam() {
        Grade grade = Grade.restore(1L, 2L, 3L, "20260001", new BigDecimal("90.00"), BigDecimal.ONE, new BigDecimal("90.00"), BigDecimal.ZERO, "{\"score\":90}", null, null, null);
        given(gradeJpaRepository.findAllBySectionIdAndTeamIdAndDeletedAtIsNull(2L, 3L))
                .willReturn(List.of(GradeJpaEntity.toEntity(grade)));
        GradeRepositoryImpl repository = new GradeRepositoryImpl(gradeJpaRepository);

        List<Grade> result = repository.findAllBySectionIdAndTeamId(2L, 3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("20260001");
    }

    @Test
    @DisplayName("성적 저장소 어댑터는 분반과 팀과 학번 조합으로 성적을 조회한다")
    void findGradeByUser() {
        Grade grade = Grade.restore(1L, 2L, 3L, "20260001", new BigDecimal("90.00"), BigDecimal.ONE, new BigDecimal("90.00"), BigDecimal.ZERO, "{\"score\":90}", null, null, null);
        given(gradeJpaRepository.findBySectionIdAndTeamIdAndUserIdAndDeletedAtIsNull(2L, 3L, "20260001"))
                .willReturn(Optional.of(GradeJpaEntity.toEntity(grade)));
        GradeRepositoryImpl repository = new GradeRepositoryImpl(gradeJpaRepository);

        Optional<Grade> result = repository.findBySectionIdAndTeamIdAndUserId(2L, 3L, " 20260001 ");

        assertThat(result).isPresent();
        assertThat(result.get().getFinalScore()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("상호평가 팀원 답변 저장소 어댑터는 제출 ID로 삭제 후 플러시한다")
    void deleteAllTeammateAnswersBySubmissionId() {
        PeerEvaluationTeammateAnswerRepositoryImpl repository = new PeerEvaluationTeammateAnswerRepositoryImpl(teammateAnswerJpaRepository);

        repository.deleteAllBySubmissionId(10L);

        verify(teammateAnswerJpaRepository).deleteAllBySubmissionId(10L);
        verify(teammateAnswerJpaRepository).flush();
    }

    @Test
    @DisplayName("상호평가 팀원 답변 저장소 어댑터는 답변들을 일괄 저장한다")
    void saveAllTeammateAnswers() {
        PeerEvaluationTeammateAnswer answer = PeerEvaluationTeammateAnswer.create(10L, "20260002", 100, "기여도", "평가");
        given(teammateAnswerJpaRepository.saveAll(any())).willReturn(List.of(PeerEvaluationTeammateAnswerJpaEntity.from(answer)));
        PeerEvaluationTeammateAnswerRepositoryImpl repository = new PeerEvaluationTeammateAnswerRepositoryImpl(teammateAnswerJpaRepository);

        List<PeerEvaluationTeammateAnswer> results = repository.saveAll(List.of(answer));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTargetUserId()).isEqualTo("20260002");
    }

    @Test
    @DisplayName("상호평가 팀원 답변 저장소 어댑터는 제출 ID로 답변 목록을 조회한다")
    void findAllTeammateAnswersBySubmissionId() {
        PeerEvaluationTeammateAnswer answer = PeerEvaluationTeammateAnswer.create(10L, "20260002", 100, "기여도", "평가");
        given(teammateAnswerJpaRepository.findAllBySubmissionIdOrderById(10L))
            .willReturn(List.of(PeerEvaluationTeammateAnswerJpaEntity.from(answer)));
        PeerEvaluationTeammateAnswerRepositoryImpl repository = new PeerEvaluationTeammateAnswerRepositoryImpl(teammateAnswerJpaRepository);

        List<PeerEvaluationTeammateAnswer> results = repository.findAllBySubmissionId(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTargetUserId()).isEqualTo("20260002");
    }

    @Test
    @DisplayName("상호평가 팀원 답변 저장소 어댑터는 제출 ID 목록으로 답변 목록을 일괄 조회한다")
    void findAllTeammateAnswersBySubmissionIdIn() {
        PeerEvaluationTeammateAnswer answer1 = PeerEvaluationTeammateAnswer.create(10L, "20260002", 50, "기여도1", "평가1");
        PeerEvaluationTeammateAnswer answer2 = PeerEvaluationTeammateAnswer.create(20L, "20260003", 50, "기여도2", "평가2");
        given(teammateAnswerJpaRepository.findAllBySubmissionIdInOrderById(List.of(10L, 20L)))
            .willReturn(List.of(
                PeerEvaluationTeammateAnswerJpaEntity.from(answer1),
                PeerEvaluationTeammateAnswerJpaEntity.from(answer2)
            ));
        PeerEvaluationTeammateAnswerRepositoryImpl repository = new PeerEvaluationTeammateAnswerRepositoryImpl(teammateAnswerJpaRepository);

        List<PeerEvaluationTeammateAnswer> results = repository.findAllBySubmissionIdIn(List.of(10L, 20L));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(PeerEvaluationTeammateAnswer::getTargetUserId)
            .containsExactly("20260002", "20260003");
    }

    @Test
    @DisplayName("상호평가 제출 저장소 어댑터는 폼 ID로 전체 제출 목록을 조회한다")
    void findAllSubmissionsByFormId() {
        PeerEvaluationSubmission submission = PeerEvaluationSubmission.create(1L, "20260001");
        given(submissionJpaRepository.findAllByFormIdAndDeletedAtIsNull(1L))
            .willReturn(List.of(PeerEvaluationSubmissionJpaEntity.from(submission)));
        PeerEvaluationSubmissionRepositoryImpl repository = new PeerEvaluationSubmissionRepositoryImpl(submissionJpaRepository);

        List<PeerEvaluationSubmission> results = repository.findAllByFormId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEvaluatorId()).isEqualTo("20260001");
    }

    @Test
    @DisplayName("상호평가 제출 저장소 어댑터는 폼 ID와 평가자 학번 목록으로 제출 목록을 조회한다")
    void findAllSubmissionsByFormIdAndEvaluatorIdIn() {
        PeerEvaluationSubmission submission = PeerEvaluationSubmission.create(1L, "20260001");
        given(submissionJpaRepository.findAllByFormIdAndEvaluatorIdInAndDeletedAtIsNull(1L, List.of("20260001")))
            .willReturn(List.of(PeerEvaluationSubmissionJpaEntity.from(submission)));
        PeerEvaluationSubmissionRepositoryImpl repository = new PeerEvaluationSubmissionRepositoryImpl(submissionJpaRepository);

        List<PeerEvaluationSubmission> results = repository.findAllByFormIdAndEvaluatorIdIn(1L, List.of("20260001"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEvaluatorId()).isEqualTo("20260001");
    }
}
