package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PeerEvaluationFormCommandServiceTest {

    @Mock
    private PeerEvaluationFormRepository formRepository;

    @InjectMocks
    private PeerEvaluationFormCommandService commandService;

    @Test
    @DisplayName("상호평가 양식을 생성하면 저장된 id를 반환한다")
    void createForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        given(formRepository.save(any(PeerEvaluationForm.class)))
                .willReturn(PeerEvaluationForm.restore(
                        1L, 2L, 3L, true, opensAt, closesAt, null, null, null));

        Long id = commandService.createForm(2L, 3L, true, opensAt, closesAt);

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<PeerEvaluationForm> captor = ArgumentCaptor.forClass(PeerEvaluationForm.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().getSectionId()).isEqualTo(2L);
        assertThat(captor.getValue().getMilestoneId()).isEqualTo(3L);
        assertThat(captor.getValue().isAnonymous()).isTrue();
        assertThat(captor.getValue().getOpensAt()).isEqualTo(opensAt);
        assertThat(captor.getValue().getClosesAt()).isEqualTo(closesAt);
    }

    @Test
    @DisplayName("상호평가 양식을 수정하면 익명 여부와 기간이 갱신되어 저장된다")
    void updateForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm existingForm = PeerEvaluationForm.restore(
                1L, 2L, 3L, true, opensAt, closesAt, null, null, null);
        given(formRepository.findById(1L)).willReturn(Optional.of(existingForm));

        LocalDateTime newOpensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime newClosesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        commandService.updateForm(2L, 1L, false, newOpensAt, newClosesAt);

        ArgumentCaptor<PeerEvaluationForm> captor = ArgumentCaptor.forClass(PeerEvaluationForm.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().isAnonymous()).isFalse();
        assertThat(captor.getValue().getOpensAt()).isEqualTo(newOpensAt);
        assertThat(captor.getValue().getClosesAt()).isEqualTo(newClosesAt);
    }

    @Test
    @DisplayName("존재하지 않는 상호평가 양식 수정 시 예외가 발생한다")
    void updateFormNotFound() {
        given(formRepository.findById(999L)).willReturn(Optional.empty());

        LocalDateTime newOpensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime newClosesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        assertThatThrownBy(() -> commandService.updateForm(2L, 999L, false, newOpensAt, newClosesAt))
                .isInstanceOf(PeerEvaluationFormNotFoundException.class);
    }

    @Test
    @DisplayName("분반 식별자가 일치하지 않는 상호평가 양식 수정 시 예외가 발생한다")
    void updateFormSectionMismatch() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm existingForm = PeerEvaluationForm.restore(
                1L, 2L, 3L, true, opensAt, closesAt, null, null, null);
        given(formRepository.findById(1L)).willReturn(Optional.of(existingForm));

        LocalDateTime newOpensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime newClosesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        assertThatThrownBy(() -> commandService.updateForm(999L, 1L, false, newOpensAt, newClosesAt))
                .isInstanceOf(PeerEvaluationFormNotFoundException.class);
    }

    @Test
    @DisplayName("마일스톤 식별자로 기간 수정 시 양식이 존재하면 기간만 갱신된다")
    void updateFormPeriodByMilestoneIdExisting() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm existingForm = PeerEvaluationForm.restore(
                1L, 2L, 3L, true, opensAt, closesAt, null, null, null);
        given(formRepository.findByMilestoneId(3L)).willReturn(Optional.of(existingForm));

        LocalDateTime newOpensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime newClosesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        commandService.updateFormPeriodByMilestoneId(2L, 3L, newOpensAt, newClosesAt);

        ArgumentCaptor<PeerEvaluationForm> captor = ArgumentCaptor.forClass(PeerEvaluationForm.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().isAnonymous()).isTrue();
        assertThat(captor.getValue().getOpensAt()).isEqualTo(newOpensAt);
        assertThat(captor.getValue().getClosesAt()).isEqualTo(newClosesAt);
    }

    @Test
    @DisplayName("마일스톤 식별자로 기간 수정 시 양식이 없고 분반 정보가 주어지면 양식을 생성한다")
    void updateFormPeriodByMilestoneIdCreateWhenMissing() {
        given(formRepository.findByMilestoneId(3L)).willReturn(Optional.empty());

        LocalDateTime newOpensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime newClosesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        commandService.updateFormPeriodByMilestoneId(2L, 3L, newOpensAt, newClosesAt);

        ArgumentCaptor<PeerEvaluationForm> captor = ArgumentCaptor.forClass(PeerEvaluationForm.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().getSectionId()).isEqualTo(2L);
        assertThat(captor.getValue().getMilestoneId()).isEqualTo(3L);
        assertThat(captor.getValue().isAnonymous()).isTrue();
        assertThat(captor.getValue().getOpensAt()).isEqualTo(newOpensAt);
        assertThat(captor.getValue().getClosesAt()).isEqualTo(newClosesAt);
    }

    @Test
    @DisplayName("마일스톤 식별자로 기간 수정 시 양식이 없고 분반 정보가 없으면 아무 작업도 하지 않는다")
    void updateFormPeriodByMilestoneIdNoOpWhenMissingAndNoSection() {
        given(formRepository.findByMilestoneId(3L)).willReturn(Optional.empty());

        LocalDateTime newOpensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime newClosesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        commandService.updateFormPeriodByMilestoneId(null, 3L, newOpensAt, newClosesAt);

        verify(formRepository, never()).save(any());
    }
}
