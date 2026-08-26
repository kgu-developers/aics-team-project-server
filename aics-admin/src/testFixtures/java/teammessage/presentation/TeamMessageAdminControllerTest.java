package teammessage.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.admin.teammessage.application.TeamMessageAdminFacade;
import kgu.developers.admin.teammessage.presentation.TeamMessageAdminControllerImpl;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminResponse;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TeamMessageAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/messages";
    private static final String PROFESSOR_ID = "202699999";

    @Mock
    private TeamMessageAdminFacade teamMessageAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TeamMessageAdminControllerImpl(teamMessageAdminFacade))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @Test
    @DisplayName("GET /messages는 분반 필터와 인증된 교수 학번을 전달한다")
    void getMessages_WithSectionFilter() throws Exception {
        given(teamMessageAdminFacade.getMessages(eq(1L), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(response());

        mockMvc.perform(get(BASE_URL)
                .param("sectionId", "1")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].sectionName").value("1151"))
            .andExpect(jsonPath("$.contents[0].teamName").value("A팀"))
            .andExpect(jsonPath("$.unreadCount").value(12));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamMessageAdminFacade).getMessages(eq(1L), pageableCaptor.capture(), eq(PROFESSOR_ID));
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
    }

    private TeamMessageAdminPageResponse response() {
        TeamMessageAdminResponse content = TeamMessageAdminResponse.builder()
            .id(1L)
            .sectionId(1L)
            .sectionName("1151")
            .teamId(10L)
            .teamName("A팀")
            .senderId("202612345")
            .message("화면설계서 확인 부탁드립니다.")
            .relatedType(TeamMessageRelatedType.GENERAL)
            .createdAt("2026-08-25 19:30")
            .build();
        return TeamMessageAdminPageResponse.builder()
            .contents(List.of(content))
            .unreadCount(12L)
            .pageable(PageableResponse.<TeamMessageAdminResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1)
                .isEnd(true)
                .build())
            .build();
    }
}
