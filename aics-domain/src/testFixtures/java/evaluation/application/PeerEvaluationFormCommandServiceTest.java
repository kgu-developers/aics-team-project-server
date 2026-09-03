package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
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
}
