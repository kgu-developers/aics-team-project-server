package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PeerEvaluationFormFacadeTest {

    @Mock
    private PeerEvaluationFormCommandService commandService;

    @Mock
    private SectionQueryService sectionQueryService;

    @InjectMocks
    private PeerEvaluationFormFacade facade;

    @Test
    @DisplayName("상호평가 양식 생성 요청을 커맨드 서비스에 전달하고 id를 응답한다")
    void createForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.getSectionById(2L)).willReturn(sectionOwnedBy("202012345"));
        given(commandService.createForm(2L, 3L, true, opensAt, closesAt)).willReturn(1L);

        assertThat(facade.createForm(2L, "202012345", request).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("담당 교수가 아닌 관리자는 상호평가 양식을 생성할 수 없다")
    void rejectAnotherProfessor() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.getSectionById(2L)).willReturn(sectionOwnedBy("another-professor"));

        assertThatThrownBy(() -> facade.createForm(2L, "202012345", request))
                .isInstanceOf(AccessDeniedException.class);

        then(commandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 분반에는 상호평가 양식을 생성하지 않는다")
    void rejectMissingSection() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.getSectionById(404L)).willThrow(new SectionNotFoundException());

        assertThatThrownBy(() -> facade.createForm(404L, "202012345", request))
                .isInstanceOf(SectionNotFoundException.class);

        then(commandService).shouldHaveNoInteractions();
    }

    private SectionDetail sectionOwnedBy(String professorId) {
        Section section = Section.builder()
                .id(2L)
                .professorId(professorId)
                .build();
        return new SectionDetail(section, null, null);
    }
}
