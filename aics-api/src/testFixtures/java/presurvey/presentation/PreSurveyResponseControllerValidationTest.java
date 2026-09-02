package presurvey.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kgu.developers.api.preSurveyResponse.application.PreSurveyResponseFacade;
import kgu.developers.api.preSurveyResponse.presentation.PreSurveyResponseControllerImpl;
import kgu.developers.common.exception.GlobalExceptionHandler;

@WebMvcTest
@Import({PreSurveyResponseControllerImpl.class, GlobalExceptionHandler.class})
@WithMockUser(username = "202012345")
class PreSurveyResponseControllerValidationTest {

	private static final String SUBMIT_URL = "/api/v1/oop/sections/{sectionId}/pre-survey/responses";

	@SpringBootConfiguration
	static class TestApp {
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PreSurveyResponseFacade preSurveyResponseFacade;

	// 빈 배열뿐 아니라 원소가 빈 값인 경우도 막혀야 한다. preferred_roles 는 NOT NULL JSONB 라
	// [null] 이 통과하면 팀 배정이 역할 없는 응답을 읽게 된다.
	@ParameterizedTest(name = "preferredRoles={0}")
	@ValueSource(strings = {"[]", "[null]", "[\"  \"]", "[\"BACKEND\", \"\"]"})
	@DisplayName("희망 역할이 비었거나 빈 원소를 포함하면 400을 응답하고 제출까지 가지 않는다")
	void rejectsBlankPreferredRoles(String preferredRoles) throws Exception {
		mockMvc.perform(post(SUBMIT_URL, 1L)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"preferredRoles\":%s}".formatted(preferredRoles)))
				.andExpect(status().isBadRequest());

		then(preSurveyResponseFacade).should(never()).submit(anyLong(), anyString(), any());
	}
}
