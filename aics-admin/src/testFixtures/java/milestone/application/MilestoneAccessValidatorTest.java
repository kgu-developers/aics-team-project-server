package milestone.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.admin.milestone.application.MilestoneAccessValidator;
import kgu.developers.domain.section.application.query.SectionQueryService;

@ExtendWith(MockitoExtension.class)
class MilestoneAccessValidatorTest {

    @Mock
    private SectionQueryService sectionQueryService;

    @InjectMocks
    private MilestoneAccessValidator milestoneAccessValidator;

    @Test
    @DisplayName("담당 교수가 아닌 사용자의 분반 마일스톤 접근을 거부한다")
    void rejectAnotherProfessorsSection() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(1L, "202012345"))
                .willReturn(false);

        assertThatThrownBy(() -> milestoneAccessValidator.validateSectionAccess(1L, "202012345"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("담당 교수");
    }
}
