package meetingrecord.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import jakarta.validation.ConstraintViolationException;
import kgu.developers.admin.meetingrecord.application.MeetingRecordAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.MeetingRecordAdminController;
import kgu.developers.admin.meetingrecord.presentation.MeetingRecordAdminControllerImpl;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordAdminPageResponse;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordAdminResponse;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@ExtendWith(MockitoExtension.class)
class MeetingRecordAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/meeting-records";
    private static final String PROFESSOR_ID = "202699999";

    @Mock
    private MeetingRecordAdminFacade meetingRecordAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeetingRecordAdminControllerImpl(meetingRecordAdminFacade))
            .build();
    }

    @Test
    @DisplayName("GET /meeting-records는 분반 필터와 인증된 교수 학번을 전달한다")
    void getMeetingRecords_WithSectionFilter() throws Exception {
        given(meetingRecordAdminFacade.getMeetingRecords(eq(1L), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(response());

        mockMvc.perform(get(BASE_URL)
                .param("sectionId", "1")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].sectionName").value("1151"))
            .andExpect(jsonPath("$.contents[0].teamName").value("A팀"))
            .andExpect(jsonPath("$.pageable.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordAdminFacade).getMeetingRecords(eq(1L), pageableCaptor.capture(), eq(PROFESSOR_ID));
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("GET /meeting-records는 0 이하의 분반 식별자를 거부한다")
    void getMeetingRecords_WithInvalidSectionId() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        processor.setProxyTargetClass(true);
        processor.afterPropertiesSet();
        MeetingRecordAdminController controller =
            (MeetingRecordAdminController) processor.postProcessAfterInitialization(
                new MeetingRecordAdminControllerImpl(meetingRecordAdminFacade),
                "meetingRecordAdminController");

        assertThatThrownBy(() -> controller.getMeetingRecords(
            -1L,
            0,
            20,
            new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(meetingRecordAdminFacade);
    }

    private MeetingRecordAdminPageResponse response() {
        MeetingRecordAdminResponse content = MeetingRecordAdminResponse.builder()
            .id(1L)
            .sectionId(1L)
            .sectionName("1151")
            .teamId(10L)
            .teamName("A팀")
            .phase(MeetingPhase.MID_CHECK)
            .authorId("202612345")
            .meetingAt("2026-08-25 19:30")
            .content("와이어프레임 기획 논의")
            .participantCount(4)
            .build();
        return MeetingRecordAdminPageResponse.builder()
            .contents(List.of(content))
            .pageable(PageableResponse.<MeetingRecordAdminResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1)
                .isEnd(true)
                .build())
            .build();
    }
}
