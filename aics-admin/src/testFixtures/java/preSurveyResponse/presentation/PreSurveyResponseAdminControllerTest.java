package preSurveyResponse.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseAdminFacade;
import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseExcelDownload;
import kgu.developers.admin.preSurveyResponse.presentation.PreSurveyResponseAdminControllerImpl;
import kgu.developers.common.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class PreSurveyResponseAdminControllerTest {

    private static final String DOWNLOAD_URL = "/api/v1/admin/oop/sections/1/pre-survey-responses/download";
    private static final String PROFESSOR_ID = "202699999";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private PreSurveyResponseAdminFacade preSurveyResponseAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new PreSurveyResponseAdminControllerImpl(preSurveyResponseAdminFacade))
            .setControllerAdvice(new GlobalExceptionHandler(event -> { }))
            .build();
    }

    @Test
    @DisplayName("엑셀 다운로드는 xlsx 본문과 첨부 파일명·캐시 금지 헤더를 함께 내려준다")
    void downloadResponsesExcel_WritesDownloadHeaders() throws Exception {
        given(preSurveyResponseAdminFacade.downloadResponsesExcel(1L, PROFESSOR_ID)).willReturn(
            new PreSurveyResponseExcelDownload("객체지향프로그래밍 01-사전조사.xlsx", new byte[] {1, 2, 3}));

        mockMvc.perform(get(DOWNLOAD_URL)
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX))
            .andExpect(content().bytes(new byte[] {1, 2, 3}))
            // 한글 파일명이라 RFC 5987 형식(filename*)으로 인코딩된다
            .andExpect(header().string("Content-Disposition", startsWith("attachment;")))
            .andExpect(header().string("Content-Disposition", containsString(
                "filename*=UTF-8''%EA%B0%9D%EC%B2%B4%EC%A7%80%ED%96%A5%ED%94%84%EB%A1%9C"
                    + "%EA%B7%B8%EB%9E%98%EB%B0%8D%2001-%EC%82%AC%EC%A0%84%EC%A1%B0%EC%82%AC.xlsx")))
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    @DisplayName("담당 분반이 아니면 엑셀 다운로드는 403이다")
    void downloadResponsesExcel_RejectsNonOwningProfessor() throws Exception {
        willThrow(new AccessDeniedException("담당 분반의 사전조사 응답만 조회할 수 있습니다."))
            .given(preSurveyResponseAdminFacade).downloadResponsesExcel(1L, PROFESSOR_ID);

        mockMvc.perform(get(DOWNLOAD_URL)
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isForbidden());
    }
}
