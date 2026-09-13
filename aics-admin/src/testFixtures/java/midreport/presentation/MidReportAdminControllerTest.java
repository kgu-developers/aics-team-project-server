package midreport.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.admin.midreport.application.MidReportAdminFacade;
import kgu.developers.admin.midreport.presentation.MidReportAdminController;
import kgu.developers.admin.midreport.presentation.MidReportAdminControllerImpl;
import kgu.developers.admin.midreport.presentation.request.MidReportFeedbackAdminRequest;
import kgu.developers.admin.midreport.presentation.response.MidReportAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminPageResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportRevisionAdminResponse;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@ExtendWith(MockitoExtension.class)
class MidReportAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/sections/1/teams/10/mid-report";
    private static final String PROFESSOR_ID = "202699999";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private MidReportAdminFacade midReportAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MidReportAdminControllerImpl(midReportAdminFacade))
            .build();
    }

    @Test
    @DisplayName("GET /mid-report는 인증된 교수 학번과 함께 중간보고서 상세를 조회한다")
    void getMidReport_Success() throws Exception {
        MidReportAdminResponse response = MidReportAdminResponse.builder()
            .id(100L)
            .teamId(10L)
            .teamName("A팀")
            .milestoneId(5L)
            .title("A팀 중간보고서")
            .version(1L)
            .status(MidReportStatus.SUBMITTED)
            .dueDate(LocalDateTime.of(2026, 9, 30, 23, 59))
            .submittedAt(LocalDateTime.of(2026, 9, 10, 12, 0))
            .submittedBy("202412345")
            .submittedByName("홍길동")
            .leaderName("홍길동")
            .revision(new MidReportRevisionAdminResponse(List.of("gui-design"), List.of(), null, null))
            .blocks(List.of())
            .build();

        given(midReportAdminFacade.getMidReport(1L, 10L, PROFESSOR_ID))
            .willReturn(response);

        mockMvc.perform(get(BASE_URL)
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.teamName").value("A팀"))
            .andExpect(jsonPath("$.leaderName").value("홍길동"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(midReportAdminFacade).getMidReport(1L, 10L, PROFESSOR_ID);
    }

    @Test
    @DisplayName("POST /mid-report/feedback은 피드백을 등록하고 등록된 피드백 정보를 반환한다")
    void postFeedback_Success() throws Exception {
        MidReportFeedbackAdminRequest request = new MidReportFeedbackAdminRequest(
            "GUI 화면을 보완해주세요.",
            List.of("gui-design")
        );
        MidReportFeedbackAdminResponse response = MidReportFeedbackAdminResponse.builder()
            .messageId(500L)
            .teamId(10L)
            .midReportId(100L)
            .senderId(PROFESSOR_ID)
            .senderName("김교수")
            .message("GUI 화면을 보완해주세요.")
            .createdAt("2026-09-13 14:00")
            .build();

        given(midReportAdminFacade.postFeedback(eq(1L), eq(10L), any(MidReportFeedbackAdminRequest.class), eq(PROFESSOR_ID)))
            .willReturn(response);

        mockMvc.perform(post(BASE_URL + "/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value(500))
            .andExpect(jsonPath("$.senderName").value("김교수"))
            .andExpect(jsonPath("$.message").value("GUI 화면을 보완해주세요."));

        verify(midReportAdminFacade).postFeedback(eq(1L), eq(10L), eq(request), eq(PROFESSOR_ID));
    }

    @Test
    @DisplayName("POST /mid-report/feedback은 빈 메시지를 거부한다")
    void postFeedback_EmptyMessage_FailsValidation() throws Exception {
        MidReportFeedbackAdminRequest request = new MidReportFeedbackAdminRequest(
            "",
            List.of("gui-design")
        );

        mockMvc.perform(post(BASE_URL + "/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(midReportAdminFacade);
    }

    @Test
    @DisplayName("GET /mid-report/feedbacks는 페이징 파라미터와 함께 피드백 목록을 조회한다")
    void getFeedbacks_Success() throws Exception {
        MidReportFeedbackAdminResponse feedback = MidReportFeedbackAdminResponse.builder()
            .messageId(500L)
            .teamId(10L)
            .midReportId(100L)
            .senderId(PROFESSOR_ID)
            .senderName("김교수")
            .message("피드백 내용")
            .createdAt("2026-09-13 14:00")
            .build();
        MidReportFeedbackAdminPageResponse pageResponse = MidReportFeedbackAdminPageResponse.builder()
            .contents(List.of(feedback))
            .pageable(PageableResponse.<MidReportFeedbackAdminResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1L)
                .isEnd(true)
                .build())
            .build();

        given(midReportAdminFacade.getFeedbacks(eq(1L), eq(10L), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(pageResponse);

        mockMvc.perform(get(BASE_URL + "/feedbacks")
                .param("page", "0")
                .param("size", "20")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].messageId").value(500))
            .andExpect(jsonPath("$.contents[0].senderName").value("김교수"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(midReportAdminFacade).getFeedbacks(eq(1L), eq(10L), pageableCaptor.capture(), eq(PROFESSOR_ID));
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /mid-report/feedbacks는 100을 초과하는 페이지 크기를 거부한다")
    void getFeedbacks_WithOversizedPage() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        processor.setProxyTargetClass(true);
        processor.afterPropertiesSet();
        MidReportAdminController controller = (MidReportAdminController) processor.postProcessAfterInitialization(
            new MidReportAdminControllerImpl(midReportAdminFacade), "midReportAdminController");

        assertThatThrownBy(() -> controller.getFeedbacks(
            1L,
            10L,
            0,
            101,
            new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(midReportAdminFacade);
    }
}
