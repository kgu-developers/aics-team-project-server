package importstatus.presentation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import jakarta.validation.ConstraintViolationException;
import kgu.developers.admin.importstatus.application.RosterImportStatusFacade;
import kgu.developers.admin.importstatus.presentation.RosterImportStatusController;
import kgu.developers.admin.importstatus.presentation.RosterImportStatusControllerImpl;
import kgu.developers.admin.importstatus.presentation.response.RosterImportAppliedResponse;
import kgu.developers.admin.importstatus.presentation.response.RosterImportStatusResponse;

@ExtendWith(MockitoExtension.class)
class RosterImportStatusControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/sections/1/roster-import-status";
    private static final String USER_ID = "202699999";

    @Mock
    private RosterImportStatusFacade rosterImportStatusFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(USER_ID, null));
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new RosterImportStatusControllerImpl(rosterImportStatusFacade))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /roster-import-status는 인증 사용자로 반영 현황을 조회한다")
    void getStatus() throws Exception {
        RosterImportStatusResponse response = new RosterImportStatusResponse(
            new RosterImportAppliedResponse("학생명단.xlsx", LocalDateTime.of(2026, 9, 9, 14, 30)),
            null);
        given(rosterImportStatusFacade.getStatus(1L, USER_ID)).willReturn(response);

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentRoster.fileName").value("학생명단.xlsx"))
            .andExpect(jsonPath("$.studentRoster.appliedAt").value("2026-09-09T14:30:00"))
            .andExpect(jsonPath("$.teamRoster").doesNotExist());

        verify(rosterImportStatusFacade).getStatus(1L, USER_ID);
    }

    @Test
    @DisplayName("GET /roster-import-status는 0 이하의 분반 식별자를 거부한다")
    void getStatus_InvalidSectionId() {
        RosterImportStatusController controller = validatedController();

        assertThatThrownBy(() -> controller.getStatus(0L))
            .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(rosterImportStatusFacade);
    }

    private RosterImportStatusController validatedController() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        processor.setProxyTargetClass(true);
        processor.afterPropertiesSet();
        return (RosterImportStatusController) processor.postProcessAfterInitialization(
            new RosterImportStatusControllerImpl(rosterImportStatusFacade),
            "rosterImportStatusController");
    }
}
