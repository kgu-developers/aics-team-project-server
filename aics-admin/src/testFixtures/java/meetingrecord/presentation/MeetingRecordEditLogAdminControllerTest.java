package meetingrecord.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.admin.meetingrecord.application.MeetingRecordEditLogAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.MeetingRecordEditLogAdminControllerImpl;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordEditLogAdminPageResponse;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordEditLogAdminResponse;
import kgu.developers.common.response.PageableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MeetingRecordEditLogAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/sections/1/meeting-records/logs";
    private static final String PROFESSOR_ID = "202699999";

    @Mock
    private MeetingRecordEditLogAdminFacade meetingRecordEditLogAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new MeetingRecordEditLogAdminControllerImpl(meetingRecordEditLogAdminFacade))
            .build();
    }

    @Test
    @DisplayName("GET은 분반 식별자와 인증된 교수 학번을 전달하고 기본 페이지 조건을 쓴다")
    void getSectionMeetingRecordLogs_Default() throws Exception {
        given(meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            eq(1L), isNull(), isNull(), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(response());

        mockMvc.perform(get(BASE_URL)
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].teamName").value("2팀"))
            .andExpect(jsonPath("$.contents[0].meetingRecordTitle").value("3주차 정기 회의"))
            .andExpect(jsonPath("$.pageable.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordEditLogAdminFacade).getSectionMeetingRecordLogs(
            eq(1L), isNull(), isNull(), captor.capture(), eq(PROFESSOR_ID));
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET은 팀·회의록 필터와 페이지 파라미터를 그대로 전달한다")
    void getSectionMeetingRecordLogs_WithFilters() throws Exception {
        given(meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
            eq(1L), eq(20L), eq(7L), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(response());

        mockMvc.perform(get(BASE_URL)
                .param("teamId", "20")
                .param("meetingRecordId", "7")
                .param("page", "1")
                .param("size", "50")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordEditLogAdminFacade).getSectionMeetingRecordLogs(
            eq(1L), eq(20L), eq(7L), captor.capture(), eq(PROFESSOR_ID));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    private MeetingRecordEditLogAdminPageResponse response() {
        MeetingRecordEditLogAdminResponse content = MeetingRecordEditLogAdminResponse.builder()
            .id(3L)
            .teamId(20L)
            .teamName("2팀")
            .meetingRecordId(7L)
            .meetingRecordTitle("3주차 정기 회의")
            .editorId("202412345")
            .editorName("홍길동")
            .reason("회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 고쳤습니다.")
            .createdAt("2026-10-04 15:30")
            .build();

        return MeetingRecordEditLogAdminPageResponse.builder()
            .contents(List.of(content))
            .pageable(PageableResponse.<MeetingRecordEditLogAdminResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1)
                .isEnd(true)
                .build())
            .build();
    }
}
