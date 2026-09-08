package midreport.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.midreport.application.MidReportFacade;
import kgu.developers.api.midreport.presentation.MidReportControllerImpl;
import kgu.developers.api.midreport.presentation.request.MidReportBlockCompletionRequest;
import kgu.developers.api.midreport.presentation.request.MidReportBlockUpdateRequest;
import kgu.developers.api.midreport.presentation.request.MidReportSubmissionRequest;
import kgu.developers.api.midreport.presentation.response.MidReportBlockResponse;
import kgu.developers.api.midreport.presentation.response.MidReportResponse;
import kgu.developers.domain.midreport.domain.MidReportBlockStatus;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MidReportControllerTest {
    private static final String USER_ID = "202600001";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private MidReportFacade midReportFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MidReportControllerImpl(midReportFacade)).build();
    }

    @Test
    @DisplayName("GET /mid-reports/current는 현재 중간보고서 전체 계약을 반환한다")
    void getCurrent() throws Exception {
        given(midReportFacade.getCurrent(USER_ID)).willReturn(response());

        mockMvc.perform(get("/mid-reports/current").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100L))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.blocks[0].key").value("topic"))
            .andExpect(jsonPath("$.blocks[0].lock").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /mid-reports/{id}/blocks/{blockKey}는 버전과 필드를 전달한다")
    void updateBlock() throws Exception {
        given(midReportFacade.updateBlock(eq(100L), eq("topic"), eq(USER_ID), any())).willReturn(response());
        String body = """
            {"version":0,"fields":[{"key":"title","value":"CineFlow"},{"key":"description","value":"설명"}]}
            """;

        mockMvc.perform(patch("/mid-reports/100/blocks/topic")
                .principal(authentication()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(0L));

        then(midReportFacade).should().updateBlock(eq(100L), eq("topic"), eq(USER_ID), any(MidReportBlockUpdateRequest.class));
    }

    @Test
    @DisplayName("POST 영역 완료 API는 version 누락 시 400을 반환한다")
    void completeBlockRequiresVersion() throws Exception {
        mockMvc.perform(post("/mid-reports/100/blocks/topic/completion")
                .principal(authentication()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        then(midReportFacade).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST 영역 완료 API는 확인된 경로와 요청을 facade에 전달한다")
    void completeBlock() throws Exception {
        given(midReportFacade.completeBlock(eq(100L), eq("topic"), eq(USER_ID), any())).willReturn(response());

        mockMvc.perform(post("/mid-reports/100/blocks/topic/completion")
                .principal(authentication()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isOk());

        then(midReportFacade).should().completeBlock(eq(100L), eq("topic"), eq(USER_ID), any(MidReportBlockCompletionRequest.class));
    }

    @Test
    @DisplayName("POST 최종 제출 API는 버전과 인증 사용자를 전달한다")
    void submit() throws Exception {
        given(midReportFacade.submit(eq(100L), eq(USER_ID), any())).willReturn(response());

        mockMvc.perform(post("/mid-reports/100/submit")
                .principal(authentication()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isOk());

        then(midReportFacade).should().submit(eq(100L), eq(USER_ID), any(MidReportSubmissionRequest.class));
    }

    @Test
    @DisplayName("POST 최종 제출 API는 version 누락 시 400을 반환한다")
    void submitRequiresVersion() throws Exception {
        mockMvc.perform(post("/mid-reports/100/submit")
                .principal(authentication()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        then(midReportFacade).shouldHaveNoInteractions();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null);
    }

    private MidReportResponse response() throws Exception {
        return new MidReportResponse(
            100L,
            10L,
            "CineFlow 중간보고서",
            0L,
            LocalDateTime.of(2026, 10, 26, 23, 59),
            MidReportStatus.DRAFT,
            "학생 A",
            null,
            null,
            null,
            List.of(new MidReportBlockResponse(
                "topic", "1. 주제", "설명", objectMapper.readTree("[]"), MidReportBlockStatus.IN_PROGRESS,
                null, null, null
            ))
        );
    }
}
