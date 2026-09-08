package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import java.util.List;

import kgu.developers.admin.milestone.application.MilestoneAccessValidator;
import kgu.developers.admin.milestone.application.MilestoneFacade;
import kgu.developers.admin.milestone.presentation.request.RequiredArtifactRequest;
import kgu.developers.domain.feedback.application.command.RequiredArtifactCommandService;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.feedback.exception.InvalidRequiredArtifactRequestException;
import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequiredArtifactFacadeTest {
    private static final Long SECTION_ID = 1L;
    private static final Long MILESTONE_ID = 2L;
    private static final String PROFESSOR_ID = "professor";

    @Mock MilestoneCommandService milestoneCommandService;
    @Mock MilestoneQueryService milestoneQueryService;
    @Mock MilestoneAccessValidator milestoneAccessValidator;
    @Mock RequiredArtifactCommandService requiredArtifactCommandService;
    @Mock RequiredArtifactQueryService requiredArtifactQueryService;

    @InjectMocks MilestoneFacade milestoneFacade;

    @Test
    @DisplayName("담당 분반과 마일스톤을 확인한 뒤 필수 산출물을 생성한다")
    void createRequiredArtifact() {
        RequiredArtifactRequest request = new RequiredArtifactRequest(
                RequiredArtifactType.FILE,
                "보고서",
                true,
                List.of("pdf"),
                20
        );
        given(requiredArtifactCommandService.create(
                MILESTONE_ID, RequiredArtifactType.FILE, "보고서", true, "pdf", 20
        )).willReturn(3L);

        assertThat(milestoneFacade.createRequiredArtifact(
                SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request
        ).id()).isEqualTo(3L);

        verify(milestoneAccessValidator).validateSectionAccess(SECTION_ID, PROFESSOR_ID);
        verify(milestoneQueryService).getMilestone(SECTION_ID, MILESTONE_ID);
    }

    @Test
    @DisplayName("필수 산출물 목록 조회 전에 담당 분반과 마일스톤 소속을 확인한다")
    void getRequiredArtifacts() {
        given(requiredArtifactQueryService.getRequiredArtifacts(MILESTONE_ID)).willReturn(List.of(
                RequiredArtifact.restore(
                        3L, MILESTONE_ID, RequiredArtifactType.LINK, "시연 링크", true,
                        null, null, null, null, null
                )
        ));

        assertThat(milestoneFacade.getRequiredArtifacts(
                SECTION_ID, PROFESSOR_ID, MILESTONE_ID
        ).contents()).singleElement().satisfies(response -> {
            assertThat(response.allowedExtensions()).isEmpty();
            assertThat(response.maxFileSizeMb()).isNull();
        });

        verify(milestoneAccessValidator).validateSectionAccess(SECTION_ID, PROFESSOR_ID);
        verify(milestoneQueryService).getMilestone(SECTION_ID, MILESTONE_ID);
    }

    @Test
    @DisplayName("잘못된 산출물 설정은 400 예외로 변환한다")
    void invalidRequiredArtifactRequest() {
        RequiredArtifactRequest request = new RequiredArtifactRequest(
                RequiredArtifactType.LINK,
                "시연 링크",
                true,
                List.of("pdf"),
                null
        );
        given(requiredArtifactCommandService.create(
                MILESTONE_ID, RequiredArtifactType.LINK, "시연 링크", true, "pdf", null
        )).willThrow(new IllegalArgumentException("FILE 유형만 설정 가능"));

        assertThatThrownBy(() -> milestoneFacade.createRequiredArtifact(
                SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request
        )).isInstanceOf(InvalidRequiredArtifactRequestException.class);
    }

    @Test
    @DisplayName("분반 접근이 거부되면 산출물 저장소를 조회하지 않는다")
    void rejectAnotherProfessor() {
        org.mockito.BDDMockito.willThrow(new org.springframework.security.access.AccessDeniedException("거부"))
                .given(milestoneAccessValidator)
                .validateSectionAccess(SECTION_ID, PROFESSOR_ID);

        assertThatThrownBy(() -> milestoneFacade.getRequiredArtifacts(
                SECTION_ID, PROFESSOR_ID, MILESTONE_ID
        )).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        then(requiredArtifactQueryService).shouldHaveNoInteractions();
    }
}
