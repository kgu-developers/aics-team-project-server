package milestone.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.api.milestone.application.MilestoneFacade;
import kgu.developers.api.milestone.presentation.MilestoneControllerImpl;
import kgu.developers.api.milestone.presentation.response.MilestoneListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MilestoneControllerImplTest {

    @Mock
    private MilestoneFacade milestoneFacade;

    @Mock
    private Authentication authentication;

    @Test
    @DisplayName("인증 사용자와 분반 식별자로 마일스톤 목록을 조회한다")
    void getMilestones() throws Exception {
        MilestoneListResponse expected = new MilestoneListResponse(List.of());
        given(authentication.getName()).willReturn("202612345");
        given(milestoneFacade.getMilestones(1L, "202612345")).willReturn(expected);
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new MilestoneControllerImpl(milestoneFacade))
            .build();

        mockMvc.perform(get("/api/v1/oop/sections/{sectionId}/milestones", 1L)
                .principal(authentication))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents").isArray());

        verify(milestoneFacade).getMilestones(1L, "202612345");
    }
}
