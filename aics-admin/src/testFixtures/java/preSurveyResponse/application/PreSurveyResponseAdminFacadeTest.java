package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.preSurveyResponse.application.PreSurveyResponseAdminFacade;
import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.section.application.query.SectionQueryService;

import mock.repository.FakePreSurveyResponseRepository;

class PreSurveyResponseAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final String PROFESSOR = "professor1";
    private static final String OTHER_PROFESSOR = "professor2";

    private SectionQueryService sectionQueryService;
    private FakePreSurveyResponseRepository preSurveyResponseRepository;
    private PreSurveyResponseAdminFacade preSurveyResponseAdminFacade;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        sectionQueryService = mock(SectionQueryService.class);
        preSurveyResponseRepository = new FakePreSurveyResponseRepository();

        JsonNode roles = objectMapper.readTree("[\"BACKEND\", \"PM\"]");
        preSurveyResponseRepository.save(
                PreSurveyResponse.create("202412345", SECTION_ID, roles, "학사 알림 서비스", "금요일 회의 어려움"));

        preSurveyResponseAdminFacade = new PreSurveyResponseAdminFacade(sectionQueryService, preSurveyResponseRepository);
    }

    @Test
    @DisplayName("담당 교수는 분반 사전조사 응답 목록을 조회할 수 있다")
    void getResponsesBySection_AllowsOwningProfessor() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(SECTION_ID, PROFESSOR);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).userId()).isEqualTo("202412345");
        assertThat(response.contents().get(0).topicOpinion()).isEqualTo("학사 알림 서비스");
    }

    @Test
    @DisplayName("담당 교수가 아니면 분반 사전조사 응답 목록을 조회할 수 없다")
    void getResponsesBySection_RejectsNonOwningProfessor() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> preSurveyResponseAdminFacade.getResponsesBySection(SECTION_ID, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("아직 아무도 응답하지 않은 분반은 빈 목록을 반환한다")
    void getResponsesBySection_ReturnsEmptyWhenNoResponses() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, PROFESSOR)).willReturn(true);

        PreSurveyResponseAdminListResponse response = preSurveyResponseAdminFacade.getResponsesBySection(2L, PROFESSOR);

        assertThat(response.contents()).isEmpty();
    }
}
