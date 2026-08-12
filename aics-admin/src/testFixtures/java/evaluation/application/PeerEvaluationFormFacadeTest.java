package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PeerEvaluationFormFacadeTest {

    @Mock
    private PeerEvaluationFormCommandService commandService;

    @InjectMocks
    private PeerEvaluationFormFacade facade;

    @Test
    @DisplayName("상호평가 양식 생성 요청을 커맨드 서비스에 전달하고 id를 응답한다")
    void createForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(commandService.createForm(2L, 3L, true, opensAt, closesAt)).willReturn(1L);

        assertThat(facade.createForm(2L, request).id()).isEqualTo(1L);
    }
}
