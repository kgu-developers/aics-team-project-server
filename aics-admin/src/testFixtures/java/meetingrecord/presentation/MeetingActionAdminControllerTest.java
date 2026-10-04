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
import kgu.developers.admin.meetingrecord.application.MeetingActionAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.MeetingActionAdminControllerImpl;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingActionAdminPageResponse;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingActionAdminResponse;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
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
class MeetingActionAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/sections/1/meeting-actions";
    private static final String PROFESSOR_ID = "202699999";

    @Mock
    private MeetingActionAdminFacade meetingActionAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeetingActionAdminControllerImpl(meetingActionAdminFacade))
            .build();
    }

    @Test
    @DisplayName("GET은 분반 식별자와 인증된 교수 학번을 전달하고 기본 페이지 조건을 사용한다")
    void getSectionMeetingActions_Default() throws Exception {
        given(meetingActionAdminFacade.getSectionMeetingActions(
            eq(1L), isNull(), isNull(), isNull(), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(response());

        mockMvc.perform(get(BASE_URL)
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].teamName").value("2팀"))
            .andExpect(jsonPath("$.contents[0].meetingRecordTitle").value("3주차 정기 회의"))
            .andExpect(jsonPath("$.contents[0].assigneeName").value("홍길동"))
            .andExpect(jsonPath("$.pageable.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingActionAdminFacade).getSectionMeetingActions(
            eq(1L), isNull(), isNull(), isNull(), captor.capture(), eq(PROFESSOR_ID));
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET은 팀·회의록·상태 필터를 그대로 전달한다")
    void getSectionMeetingActions_WithFilters() throws Exception {
        given(meetingActionAdminFacade.getSectionMeetingActions(
            eq(1L), eq(20L), eq(100L), eq(MeetingActionStatus.DONE), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(response());

        mockMvc.perform(get(BASE_URL)
                .param("teamId", "20")
                .param("meetingRecordId", "100")
                .param("status", "DONE")
                .param("page", "1")
                .param("size", "50")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingActionAdminFacade).getSectionMeetingActions(
            eq(1L), eq(20L), eq(100L), eq(MeetingActionStatus.DONE), captor.capture(), eq(PROFESSOR_ID));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    private MeetingActionAdminPageResponse response() {
        MeetingActionAdminResponse content = MeetingActionAdminResponse.builder()
            .id(5L)
            .teamId(20L)
            .teamName("2팀")
            .meetingRecordId(100L)
            .meetingRecordTitle("3주차 정기 회의")
            .meetingAt("2026-09-30 14:00")
            .assigneeId("202412345")
            .assigneeName("홍길동")
            .content("로그인 API 연동")
            .status(MeetingActionStatus.IN_PROGRESS)
            .dueAt("2026-10-07 23:59")
            .createdAt("2026-09-30 15:00")
            .updatedAt("2026-10-01 09:30")
            .build();

        return MeetingActionAdminPageResponse.builder()
            .contents(List.of(content))
            .pageable(PageableResponse.<MeetingActionAdminResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1)
                .isEnd(true)
                .build())
            .build();
    }
}
