package meetingrecord.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.api.meetingrecord.application.MeetingRecordEditLogFacade;
import kgu.developers.api.meetingrecord.application.MeetingRecordFacade;
import kgu.developers.api.meetingrecord.presentation.MeetingRecordControllerImpl;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordEditLogPageResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordEditLogResponse;
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
class MeetingRecordEditLogControllerTest {

    private static final String USER_ID = "202412345";

    @Mock
    private MeetingRecordFacade meetingRecordFacade;

    @Mock
    private MeetingRecordEditLogFacade meetingRecordEditLogFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new MeetingRecordControllerImpl(meetingRecordFacade, meetingRecordEditLogFacade))
            .build();
    }

    @Test
    @DisplayName("GET /meeting-records/{id}/logs는 기본 페이지 조건으로 수정 이력을 조회한다")
    void getMeetingRecordLogs_Default() throws Exception {
        given(meetingRecordEditLogFacade.getMeetingRecordLogs(eq(7L), any(Pageable.class), eq(USER_ID)))
            .willReturn(response());

        mockMvc.perform(get("/api/v1/meeting-records/{meetingRecordId}/logs", 7L)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].editorName").value("홍길동"))
            .andExpect(jsonPath("$.contents[0].createdAt").value("2026-10-04 15:30"))
            .andExpect(jsonPath("$.pageable.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordEditLogFacade).getMeetingRecordLogs(eq(7L), captor.capture(), eq(USER_ID));
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /meeting-records/{id}/logs는 page·size 파라미터를 그대로 전달한다")
    void getMeetingRecordLogs_WithPaging() throws Exception {
        given(meetingRecordEditLogFacade.getMeetingRecordLogs(eq(7L), any(Pageable.class), eq(USER_ID)))
            .willReturn(response());

        mockMvc.perform(get("/api/v1/meeting-records/{meetingRecordId}/logs", 7L)
                .param("page", "2")
                .param("size", "5")
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordEditLogFacade).getMeetingRecordLogs(eq(7L), captor.capture(), eq(USER_ID));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    private MeetingRecordEditLogPageResponse response() {
        MeetingRecordEditLogResponse content = MeetingRecordEditLogResponse.builder()
            .id(3L)
            .meetingRecordId(7L)
            .editorId(USER_ID)
            .editorName("홍길동")
            .reason("회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 고쳤습니다.")
            .createdAt("2026-10-04 15:30")
            .build();

        return MeetingRecordEditLogPageResponse.builder()
            .contents(List.of(content))
            .pageable(PageableResponse.<MeetingRecordEditLogResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1)
                .isEnd(true)
                .build())
            .build();
    }
}
