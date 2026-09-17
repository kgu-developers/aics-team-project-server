package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.Optional;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormUpdateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormResponse;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.section.application.query.SectionQueryService;
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

    @Mock
    private MilestoneQueryService milestoneQueryService;

    @Mock
    private PeerEvaluationFormRepository formRepository;

    @InjectMocks
    private PeerEvaluationFormFacade facade;

    @Test
    @DisplayName("상호평가 양식 생성 요청을 커맨드 서비스에 전달하고 id를 응답한다")
    void createForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(commandService.createForm(2L, 3L, true, opensAt, closesAt)).willReturn(1L);

        assertThat(facade.createForm(2L, "202012345", request).id()).isEqualTo(1L);

        then(milestoneQueryService).should().getMilestone(2L, 3L);
    }

    @Test
    @DisplayName("담당 교수가 아닌 관리자는 상호평가 양식을 생성할 수 없다")
    void rejectAnotherProfessor() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> facade.createForm(2L, "202012345", request))
                .isInstanceOf(AccessDeniedException.class);

        then(milestoneQueryService).shouldHaveNoInteractions();
        then(commandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 분반도 권한 없음으로 처리한다")
    void rejectMissingSection() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(404L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> facade.createForm(404L, "202012345", request))
                .isInstanceOf(AccessDeniedException.class);

        then(milestoneQueryService).shouldHaveNoInteractions();
        then(commandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 마일스톤에는 상호평가 양식을 생성하지 않는다")
    void rejectMissingMilestone() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(404L, true, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(milestoneQueryService.getMilestone(2L, 404L))
                .willThrow(new MilestoneNotFoundException(404L));

        assertThatThrownBy(() -> facade.createForm(2L, "202012345", request))
                .isInstanceOf(MilestoneNotFoundException.class);

        then(commandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 분반의 마일스톤에는 상호평가 양식을 생성하지 않는다")
    void rejectMilestoneFromAnotherSection() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationFormCreateRequest request =
                new PeerEvaluationFormCreateRequest(3L, true, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(milestoneQueryService.getMilestone(2L, 3L))
                .willThrow(new MilestoneNotFoundException(3L));

        assertThatThrownBy(() -> facade.createForm(2L, "202012345", request))
                .isInstanceOf(MilestoneNotFoundException.class);

        then(commandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상호평가 양식 수정 요청을 커맨드 서비스에 전달한다")
    void updateForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        PeerEvaluationFormUpdateRequest request =
                new PeerEvaluationFormUpdateRequest(false, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);

        facade.updateForm(2L, "202012345", 1L, request);

        then(commandService).should().updateForm(2L, 1L, false, opensAt, closesAt);
    }

    @Test
    @DisplayName("담당 교수가 아닌 관리자는 상호평가 양식을 수정할 수 없다")
    void rejectAnotherProfessorOnUpdate() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        PeerEvaluationFormUpdateRequest request =
                new PeerEvaluationFormUpdateRequest(false, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> facade.updateForm(2L, "202012345", 1L, request))
                .isInstanceOf(AccessDeniedException.class);

        then(commandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("양식 식별자로 상호평가 양식을 조회한다")
    void getForm() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                1L, 2L, 3L, true, opensAt, closesAt, null, null, null);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(formRepository.findById(1L)).willReturn(Optional.of(form));

        PeerEvaluationFormResponse response = facade.getForm(2L, "202012345", 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sectionId()).isEqualTo(2L);
        assertThat(response.milestoneId()).isEqualTo(3L);
        assertThat(response.anonymous()).isTrue();
        assertThat(response.opensAt()).isEqualTo(opensAt);
        assertThat(response.closesAt()).isEqualTo(closesAt);
    }

    @Test
    @DisplayName("양식 식별자 조회 시 존재하지 않으면 예외가 발생한다")
    void getFormNotFound() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(formRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getForm(2L, "202012345", 999L))
                .isInstanceOf(PeerEvaluationFormNotFoundException.class);
    }

    @Test
    @DisplayName("양식 식별자 조회 시 분반이 일치하지 않으면 예외가 발생한다")
    void getFormSectionMismatch() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                1L, 999L, 3L, true, opensAt, closesAt, null, null, null);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(formRepository.findById(1L)).willReturn(Optional.of(form));

        assertThatThrownBy(() -> facade.getForm(2L, "202012345", 1L))
                .isInstanceOf(PeerEvaluationFormNotFoundException.class);
    }

    @Test
    @DisplayName("담당 교수가 아닌 관리자는 양식 식별자로 조회할 수 없다")
    void getFormAnotherProfessorForbidden() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> facade.getForm(2L, "202012345", 1L))
                .isInstanceOf(AccessDeniedException.class);

        then(formRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마일스톤 식별자로 상호평가 양식을 조회한다")
    void getFormByMilestoneId() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                1L, 2L, 3L, true, opensAt, closesAt, null, null, null);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(formRepository.findByMilestoneId(3L)).willReturn(Optional.of(form));

        PeerEvaluationFormResponse response = facade.getFormByMilestoneId(2L, "202012345", 3L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.milestoneId()).isEqualTo(3L);
        assertThat(response.anonymous()).isTrue();
        then(milestoneQueryService).should().getMilestone(2L, 3L);
    }

    @Test
    @DisplayName("마일스톤 식별자 조회 시 양식이 없으면 예외가 발생한다")
    void getFormByMilestoneIdNotFound() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(formRepository.findByMilestoneId(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getFormByMilestoneId(2L, "202012345", 3L))
                .isInstanceOf(PeerEvaluationFormNotFoundException.class);
        then(milestoneQueryService).should().getMilestone(2L, 3L);
    }

    @Test
    @DisplayName("마일스톤 식별자 조회 시 분반이 일치하지 않으면 예외가 발생한다")
    void getFormByMilestoneIdSectionMismatch() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 8, 23, 59);
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                1L, 999L, 3L, true, opensAt, closesAt, null, null, null);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);
        given(formRepository.findByMilestoneId(3L)).willReturn(Optional.of(form));

        assertThatThrownBy(() -> facade.getFormByMilestoneId(2L, "202012345", 3L))
                .isInstanceOf(PeerEvaluationFormNotFoundException.class);
        then(milestoneQueryService).should().getMilestone(2L, 3L);
    }

    @Test
    @DisplayName("담당 교수가 아닌 관리자는 마일스톤 식별자로 조회할 수 없다")
    void getFormByMilestoneIdAnotherProfessorForbidden() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> facade.getFormByMilestoneId(2L, "202012345", 3L))
                .isInstanceOf(AccessDeniedException.class);

        then(milestoneQueryService).shouldHaveNoInteractions();
        then(formRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마일스톤 식별자로 상호평가 양식 수정 요청을 커맨드 서비스에 전달한다")
    void updateFormByMilestoneId() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        PeerEvaluationFormUpdateRequest request =
                new PeerEvaluationFormUpdateRequest(false, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(true);

        facade.updateFormByMilestoneId(2L, "202012345", 3L, request);

        then(milestoneQueryService).should().getMilestone(2L, 3L);
        then(commandService).should().updateFormByMilestoneId(2L, 3L, false, opensAt, closesAt);
    }

    @Test
    @DisplayName("담당 교수가 아닌 관리자는 마일스톤 식별자로 양식을 수정할 수 없다")
    void rejectAnotherProfessorOnUpdateByMilestoneId() {
        LocalDateTime opensAt = LocalDateTime.of(2026, 10, 2, 9, 0);
        LocalDateTime closesAt = LocalDateTime.of(2026, 10, 9, 23, 59);
        PeerEvaluationFormUpdateRequest request =
                new PeerEvaluationFormUpdateRequest(false, opensAt, closesAt);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> facade.updateFormByMilestoneId(2L, "202012345", 3L, request))
                .isInstanceOf(AccessDeniedException.class);

        then(milestoneQueryService).shouldHaveNoInteractions();
        then(commandService).shouldHaveNoInteractions();
    }
}
