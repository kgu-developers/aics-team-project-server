package proposal.presentation;

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
import java.util.List;
import kgu.developers.admin.proposal.application.ProposalAdminFacade;
import kgu.developers.admin.proposal.presentation.ProposalAdminController;
import kgu.developers.admin.proposal.presentation.ProposalAdminControllerImpl;
import kgu.developers.admin.proposal.presentation.request.ProposalFeedbackAdminRequest;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminPageResponse;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminResponse;
import kgu.developers.common.response.PageableResponse;
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
class ProposalAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/sections/1/teams/10/proposal";
    private static final String PROFESSOR_ID = "202699999";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ProposalAdminFacade proposalAdminFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProposalAdminControllerImpl(proposalAdminFacade))
            .build();
    }

    @Test
    @DisplayName("POST /proposal/feedback은 피드백을 등록하고 등록된 피드백 정보를 반환한다")
    void postFeedback_Success() throws Exception {
        ProposalFeedbackAdminRequest request = new ProposalFeedbackAdminRequest("데이터 수집 방법을 구체적으로 적어 주세요.");
        ProposalFeedbackAdminResponse response = feedbackResponse("데이터 수집 방법을 구체적으로 적어 주세요.");

        given(proposalAdminFacade.postFeedback(eq(1L), eq(10L), any(ProposalFeedbackAdminRequest.class), eq(PROFESSOR_ID)))
            .willReturn(response);

        mockMvc.perform(post(BASE_URL + "/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageId").value(500))
            .andExpect(jsonPath("$.projectId").value(100))
            .andExpect(jsonPath("$.senderName").value("김교수"))
            .andExpect(jsonPath("$.message").value("데이터 수집 방법을 구체적으로 적어 주세요."));

        verify(proposalAdminFacade).postFeedback(eq(1L), eq(10L), eq(request), eq(PROFESSOR_ID));
    }

    @Test
    @DisplayName("POST /proposal/feedback은 공백뿐인 메시지를 거부한다")
    void postFeedback_BlankMessage_FailsValidation() throws Exception {
        ProposalFeedbackAdminRequest request = new ProposalFeedbackAdminRequest("   ");

        mockMvc.perform(post(BASE_URL + "/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(proposalAdminFacade);
    }

    @Test
    @DisplayName("POST /proposal/feedback은 2000자를 초과하는 메시지를 거부한다")
    void postFeedback_TooLongMessage_FailsValidation() throws Exception {
        ProposalFeedbackAdminRequest request = new ProposalFeedbackAdminRequest("가".repeat(2001));

        mockMvc.perform(post(BASE_URL + "/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(proposalAdminFacade);
    }

    @Test
    @DisplayName("POST /proposal/feedback은 양수가 아닌 분반 식별자를 거부한다")
    void postFeedback_NonPositiveSectionId_FailsValidation() {
        ProposalAdminController controller = validatingController();

        assertThatThrownBy(() -> controller.postFeedback(
            0L,
            10L,
            new ProposalFeedbackAdminRequest("피드백 내용"),
            new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(proposalAdminFacade);
    }

    @Test
    @DisplayName("GET /proposal/feedbacks는 페이징 파라미터와 함께 피드백 목록을 조회한다")
    void getFeedbacks_Success() throws Exception {
        ProposalFeedbackAdminPageResponse pageResponse = ProposalFeedbackAdminPageResponse.builder()
            .contents(List.of(feedbackResponse("피드백 내용")))
            .pageable(PageableResponse.<ProposalFeedbackAdminResponse>builder()
                .page(0)
                .size(20)
                .totalPages(1)
                .totalElements(1L)
                .isEnd(true)
                .build())
            .build();

        given(proposalAdminFacade.getFeedbacks(eq(1L), eq(10L), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(pageResponse);

        mockMvc.perform(get(BASE_URL + "/feedbacks")
                .param("page", "0")
                .param("size", "20")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].messageId").value(500))
            .andExpect(jsonPath("$.contents[0].senderName").value("김교수"))
            .andExpect(jsonPath("$.pageable.isEnd").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(proposalAdminFacade).getFeedbacks(eq(1L), eq(10L), pageableCaptor.capture(), eq(PROFESSOR_ID));
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /proposal/feedbacks는 페이징 파라미터가 없으면 기본값(0, 20)을 사용한다")
    void getFeedbacks_DefaultPaging() throws Exception {
        given(proposalAdminFacade.getFeedbacks(eq(1L), eq(10L), any(Pageable.class), eq(PROFESSOR_ID)))
            .willReturn(ProposalFeedbackAdminPageResponse.builder()
                .contents(List.of())
                .pageable(PageableResponse.<ProposalFeedbackAdminResponse>builder()
                    .page(0)
                    .size(20)
                    .totalPages(0)
                    .totalElements(0L)
                    .isEnd(true)
                    .build())
                .build());

        mockMvc.perform(get(BASE_URL + "/feedbacks")
                .principal(new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(proposalAdminFacade).getFeedbacks(eq(1L), eq(10L), pageableCaptor.capture(), eq(PROFESSOR_ID));
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /proposal/feedbacks는 100을 초과하는 페이지 크기를 거부한다")
    void getFeedbacks_WithOversizedPage() {
        ProposalAdminController controller = validatingController();

        assertThatThrownBy(() -> controller.getFeedbacks(
            1L,
            10L,
            0,
            101,
            new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(proposalAdminFacade);
    }

    @Test
    @DisplayName("GET /proposal/feedbacks는 음수 페이지 번호를 거부한다")
    void getFeedbacks_WithNegativePage() {
        ProposalAdminController controller = validatingController();

        assertThatThrownBy(() -> controller.getFeedbacks(
            1L,
            10L,
            -1,
            20,
            new UsernamePasswordAuthenticationToken(PROFESSOR_ID, null)))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(proposalAdminFacade);
    }

    private ProposalAdminController validatingController() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        processor.setProxyTargetClass(true);
        processor.afterPropertiesSet();
        return (ProposalAdminController) processor.postProcessAfterInitialization(
            new ProposalAdminControllerImpl(proposalAdminFacade), "proposalAdminController");
    }

    private ProposalFeedbackAdminResponse feedbackResponse(String message) {
        return ProposalFeedbackAdminResponse.builder()
            .messageId(500L)
            .teamId(10L)
            .projectId(100L)
            .senderId(PROFESSOR_ID)
            .senderName("김교수")
            .message(message)
            .createdAt("2026-09-13 14:00")
            .build();
    }
}
