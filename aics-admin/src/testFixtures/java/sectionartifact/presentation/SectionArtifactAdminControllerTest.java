package sectionartifact.presentation;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import kgu.developers.admin.sectionartifact.application.SectionArtifactAdminFacade;
import kgu.developers.admin.sectionartifact.presentation.SectionArtifactAdminControllerImpl;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactMemberAdminResponse;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminListResponse;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.common.response.FileDownload;

@ExtendWith(MockitoExtension.class)
class SectionArtifactAdminControllerTest {

    private static final String SUMMARY_URL = "/api/v1/admin/sections/1/artifacts/summary";
    private static final String DOWNLOAD_URL = "/api/v1/admin/sections/1/artifacts/download";
    private static final String PROFESSOR_ID = "202699999";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 20);

    @Mock
    private SectionArtifactAdminFacade sectionArtifactAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 운영에서는 부트 자동설정이 LocalDate를 "yyyy-MM-dd"로 쓴다. standalone MockMvc는 기본
        // ObjectMapper라 숫자 배열이 되므로, 같은 설정을 붙여야 실제 응답과 같은 것을 검증한다.
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new SectionArtifactAdminControllerImpl(sectionArtifactAdminFacade))
            .setControllerAdvice(new GlobalExceptionHandler(event -> { }))
            // setMessageConverters는 기본 목록을 통째로 갈아끼우므로 xlsx(byte[]) 변환기도 같이 넣는다
            .setMessageConverters(new ByteArrayHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    @DisplayName("엑셀 다운로드는 xlsx 본문과 첨부 파일명·캐시 금지 헤더를 함께 내려준다")
    void downloadArtifactsExcel_WritesDownloadHeaders() throws Exception {
        given(sectionArtifactAdminFacade.downloadArtifactsExcel(1L, AS_OF, PROFESSOR_ID))
            .willReturn(new FileDownload("월3_OOP-01-산출물-2026-11-20.xlsx", new byte[] {1, 2, 3}));

        mockMvc.perform(get(DOWNLOAD_URL).param("asOf", "2026-11-20")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX))
            .andExpect(content().bytes(new byte[] {1, 2, 3}))
            .andExpect(header().string("Content-Disposition", startsWith("attachment;")))
            // 다른 분반 제출 현황이 캐시에 남으면 안 된다
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    @DisplayName("asOf를 생략하면 기준일 없이 파사드에 넘긴다(파사드가 오늘로 해석한다)")
    void downloadArtifactsExcel_AllowsOmittedAsOf() throws Exception {
        given(sectionArtifactAdminFacade.downloadArtifactsExcel(1L, null, PROFESSOR_ID))
            .willReturn(new FileDownload("월3_OOP-01-산출물-2026-11-20.xlsx", new byte[] {1}));

        mockMvc.perform(get(DOWNLOAD_URL)
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀별 요약 조회는 분반·기준일과 팀 목록을 JSON으로 내려준다")
    void getArtifactSummary_ReturnsSummaryJson() throws Exception {
        given(sectionArtifactAdminFacade.getArtifactSummary(1L, AS_OF, PROFESSOR_ID)).willReturn(
            SectionArtifactSummaryAdminListResponse.builder()
                .sectionId(1L)
                .sectionName("OOP-01")
                .asOf(AS_OF)
                .contents(List.of(SectionArtifactSummaryAdminResponse.builder()
                    .teamId(20L)
                    .teamName("1팀")
                    .members(List.of(SectionArtifactMemberAdminResponse.builder()
                        .studentNumber("20261234")
                        .name("김철수")
                        .build()))
                    .meetingRecordCount(8)
                    .meetingRecordEditCount(3)
                    .submittedStageCount(4)
                    .overdueMissingStageCount(1)
                    .build()))
                .build());

        mockMvc.perform(get(SUMMARY_URL).param("asOf", "2026-11-20")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sectionName").value("OOP-01"))
            .andExpect(jsonPath("$.asOf").value("2026-11-20"))
            .andExpect(jsonPath("$.contents[0].teamId").value(20))
            .andExpect(jsonPath("$.contents[0].members[0].studentNumber").value("20261234"))
            .andExpect(jsonPath("$.contents[0].meetingRecordEditCount").value(3))
            .andExpect(jsonPath("$.contents[0].overdueMissingStageCount").value(1));
    }

    @Test
    @DisplayName("담당 분반이 아니면 두 엔드포인트 모두 403이다")
    void rejectsOtherSectionStaff() throws Exception {
        willThrow(new AccessDeniedException("담당 분반의 산출물만 조회할 수 있습니다."))
            .given(sectionArtifactAdminFacade).getArtifactSummary(1L, null, PROFESSOR_ID);
        willThrow(new AccessDeniedException("담당 분반의 산출물만 조회할 수 있습니다."))
            .given(sectionArtifactAdminFacade).downloadArtifactsExcel(1L, null, PROFESSOR_ID);

        mockMvc.perform(get(SUMMARY_URL).principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(DOWNLOAD_URL).principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("기준일 형식이 잘못되면 400이다")
    void getArtifactSummary_RejectsInvalidAsOf() throws Exception {
        mockMvc.perform(get(SUMMARY_URL).param("asOf", "2026-13-99")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isBadRequest());
    }
}
